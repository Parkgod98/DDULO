import cv2
import time
import threading
import numpy as np
import os

# Qt 로그 억제
os.environ["QT_LOGGING_RULES"] = "qt.qpa.*=false"
os.environ["QT_QPA_PLATFORM"] = "xcb" 

from detector import Detector
from roi_manager import ROIManager
from mqtt_sender import MqttSender

# ==========================================
# ⚡ 설정 (1920x1080 원본)
# ==========================================
VIEW_WIDTH = 1920
VIEW_HEIGHT = 1080
AI_INTERVAL = 0.03
BUFFER_MARGIN = 40  # 1차(Green)와 2차(Yellow) 사이의 간격

# ==========================================
# 🧠 Person 클래스 (상태 머신 탑재)
# ==========================================
class Person:
    def __init__(self, p_id, anchor, polygons):
        self.id = p_id
        self.center = anchor
        self.zone = None         # 현재 카운트된 구역 이름
        self.is_valid = True     # 유효성 여부 (True: 카운트 가능, False: 무효)

        # [초기화 로직] 태어난 위치 확인
        start_in_zone = False
        for name, pts in polygons.items():
            # Green Zone(실선) 안에 있는가?
            if cv2.pointPolygonTest(pts, anchor, False) >= 0:
                start_in_zone = True
                break
        
        # ★ [핵심 1] 태어날 때부터 안이면 -> "무효(Invalid)"
        # (영상 시작 시 기존 인원 or 트래킹 끊겨서 내부에서 새로 잡힌 인원 방어)
        if start_in_zone:
            self.is_valid = False

    def update_state(self, anchor, polygons):
        self.center = anchor
        
        # 1. 현재 위치가 Green Zone(실선) 안인가?
        current_green_zone = None
        for name, pts in polygons.items():
            if cv2.pointPolygonTest(pts, anchor, False) >= 0:
                current_green_zone = name
                break

        # --- 상태 머신 로직 ---

        # [A] 현재 실선(Green) 안에 있음
        if current_green_zone is not None:
            if self.is_valid:
                # 유효한 놈이 들어옴 -> 진입 확정 (+1)
                # (이미 같은 존이면 유지, 다른 존이면 환승)
                self.zone = current_green_zone
            else:
                # 무효인 놈이 계속 안에 있음 -> 여전히 무시 (+0)
                pass

        # [B] 실선(Green) 밖임
        else:
            # 기존에 카운트 된 상태인가? (In Zone)
            if self.zone is not None:
                # 버퍼존(Yellow) 체크: Green Polygon 기준 거리 -40 이상이면 Yellow 안쪽임
                pts = polygons[self.zone]
                dist = cv2.pointPolygonTest(pts, anchor, True)
                
                if dist >= -BUFFER_MARGIN:
                    # ★ [핵심 2] Green 나갔지만 Yellow 안임 -> 상태 유지 (Holding)
                    pass 
                else:
                    # ★ [핵심 3] Yellow까지 완전히 나감 -> 이탈 (-1)
                    self.zone = None 
                    self.is_valid = True # 나갔다 들어오면 다시 세주기 위해 유효화
            
            # 카운트 안 된 상태면?
            else:
                # 밖에 있으니 이제 유효한 객체임. 들어오면 셀 준비 완료.
                self.is_valid = True

# ==========================================
# 📡 Tracker (좀비 관리자)
# ==========================================
class Tracker:
    def __init__(self):
        self.next_id = 0
        self.people = {} 

    def update(self, detections, polygons):
        used_dets = set()
        active_ids = set()
        
        # 1. 기존 ID 매칭 (거리 기반)
        for p_id, person in self.people.items():
            best_dist = 200.0 # 원본 해상도 고려 넉넉하게
            best_idx = -1
            
            for i, (cx, cy) in enumerate(detections):
                if i in used_dets: continue
                dist = np.hypot(cx - person.center[0], cy - person.center[1])
                if dist < best_dist:
                    best_dist = dist
                    best_idx = i
            
            if best_idx != -1:
                # 매칭 성공
                cx, cy = detections[best_idx]
                person.update_state((cx, cy), polygons)
                used_dets.add(best_idx)
                active_ids.add(p_id)
            else:
                # ★ [핵심 4] 매칭 실패 (가려짐)
                # 삭제하지 않음. update_state 호출 안 함 = zone 상태 그대로 유지.
                pass

        # 2. 객체 삭제 (청소)
        # 조건: "화면에 안 보임" AND "구역 밖(Zone=None)"
        remove_ids = []
        for p_id, person in self.people.items():
            if p_id not in active_ids: 
                if person.zone is None: 
                    remove_ids.append(p_id) # 볼일 다 본 놈만 삭제
        
        for p_id in remove_ids:
            del self.people[p_id]

        # 3. 신규 생성
        for i, (cx, cy) in enumerate(detections):
            if i not in used_dets:
                # 생성자가 '태생'을 확인해서 is_valid를 결정함
                new_person = Person(self.next_id, (cx, cy), polygons)
                self.people[self.next_id] = new_person
                self.next_id += 1

# ==========================================
# 🎨 시각화 도구 (Yellow Zone 생성)
# ==========================================
def get_expanded_polygon(pts, margin):
    pts = pts.reshape(-1, 2)
    M = cv2.moments(pts)
    if M['m00'] == 0: return pts
    cx = int(M['m10'] / M['m00'])
    cy = int(M['m01'] / M['m00'])
    expanded = []
    for pt in pts:
        px, py = pt
        dx = px - cx
        dy = py - cy
        dist = np.hypot(dx, dy)
        if dist == 0: expanded.append([px, py]); continue
        scale = (dist + margin) / dist
        nx = int(cx + dx * scale)
        ny = int(cy + dy * scale)
        expanded.append([nx, ny])
    return np.array(expanded, dtype=np.int32)

# ==========================================
# 🎬 Main Thread & AI Logic
# ==========================================
lock = threading.Lock()
shared_data = {
    'frame': None, 'counts': {}, 'people_draw': [],
    'polygons': {}, 'buffer_polys': {} 
}
is_running = True

def ai_thread(det, roi_man, mqtt, polys_ref):
    global shared_data, is_running
    tracker = Tracker()

    while is_running:
        try:
            if shared_data['frame'] is None:
                time.sleep(0.01); continue
            
            # 1. 감지
            frame = shared_data['frame'].copy()
            _, raw_dets = det.detect_frame(frame)
            
            # 2. 좌표 추출 (ROI Manager 사용)
            foot_points = []
            for info in raw_dets:
                # ★ 형님이 말씀하신 그 함수 사용
                anchor = roi_man.get_anchor_point(info['bbox'])
                foot_points.append(anchor)
                
            # 3. 추적 업데이트
            tracker.update(foot_points, polys_ref)
            
            # 4. 데이터 집계
            realtime_counts = {name: 0 for name in polys_ref.keys()}
            current_ppl = []
            
            for p_id, p in tracker.people.items():
                # 카운트 집계 (Zone이 있으면 무조건 셈)
                if p.zone:
                    realtime_counts[p.zone] += 1
                
                # 시각화용 매칭 (현재 보이는 놈만 박스 연결)
                best_bbox = None
                visible = False
                min_dist = 100.0
                
                for i, (fx, fy) in enumerate(foot_points):
                    dist = np.hypot(fx - p.center[0], fy - p.center[1])
                    if dist < min_dist:
                        min_dist = dist
                        best_bbox = raw_dets[i]['bbox']
                        visible = True
                
                current_ppl.append({
                    'id': p_id,
                    'bbox': best_bbox,
                    'foot': p.center,
                    'zone': p.zone,
                    'is_valid': p.is_valid,
                    'visible': visible
                })

            with lock:
                shared_data['counts'] = realtime_counts
                shared_data['people_draw'] = current_ppl
            
            try: mqtt.send_data(realtime_counts)
            except: pass
            
        except Exception as e:
            print(f"Error: {e}")
        
        time.sleep(AI_INTERVAL)

def load_polygons_raw(roi_man):
    polys = {}
    buffer_polys = {}
    for name, points in roi_man.rois.items():
        pts_np = np.array(points, dtype=np.int32)
        polys[name] = pts_np
        buffer_polys[name] = get_expanded_polygon(pts_np, BUFFER_MARGIN)
    return polys, buffer_polys

def main():
    global is_running, shared_data
    
    roi_man = ROIManager('config.yaml')
    det = Detector('models/yolov8n_best.engine', 0.25)
    mqtt = MqttSender('i14a204.p.ssafy.io', 8000, '/platform')
    try: mqtt.connect()
    except: pass

    polys, bufs = load_polygons_raw(roi_man)
    shared_data['polygons'] = polys
    shared_data['buffer_polys'] = bufs

    cap = cv2.VideoCapture("역삼역.mp4")
    
    t = threading.Thread(target=ai_thread, args=(det, roi_man, mqtt, polys))
    t.daemon = True
    t.start()
    
    cv2.namedWindow("Dashboard", cv2.WINDOW_NORMAL)
    # cv2.resizeWindow("Dashboard", 1280, 720) # 뷰어만 축소

    while True:
        ret, frame = cap.read()
        if not ret: cap.set(cv2.CAP_PROP_POS_FRAMES, 0); continue
        
        with lock:
            shared_data['frame'] = frame
            cnts = shared_data['counts']
            ppl_draw = shared_data['people_draw']
            c_polys = shared_data['polygons']
            c_bufs = shared_data['buffer_polys']
        
        draw = frame.copy()
        
        # 구역 그리기
        for name, pts in c_polys.items():
            count = cnts.get(name, 0)
            # 카운트 0보다 크면 초록, 아니면 회색
            color = (0, 255, 0) if count > 0 else (200, 200, 200)
            cv2.polylines(draw, [pts], True, color, 3) # Green Zone
            
            # Yellow Zone (Buffer)
            if name in c_bufs:
                cv2.polylines(draw, [c_bufs[name]], True, (0, 255, 255), 2, cv2.LINE_AA)
            
            cv2.putText(draw, f"{name}: {count}", (pts[0][0], pts[0][1]-15), 
                        cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 255, 0), 3)

        # 사람 그리기
        for p in ppl_draw:
            if not p['is_valid']:
                # 무효(Invalid) -> 빨간색 (태어날 때부터 안에 있던 놈)
                color = (0, 0, 255)
                status = "INV"
            elif p['zone']:
                # 유효 & 진입(In) -> 초록색
                color = (0, 255, 0)
                status = "IN"
            else:
                # 유효 & 대기(Out) -> 노란색
                color = (0, 215, 255)
                status = "OUT"

            if not p['visible']:
                # 안 보임 (Ghost) -> 회색 점 & 좌표 유지
                cv2.circle(draw, p['foot'], 5, (128,128,128), -1)
                cv2.putText(draw, f"{p['id']}(H)", p['foot'], 0, 0.7, (128,128,128), 2)
            else:
                # 보임
                if p['bbox']:
                    x1, y1, x2, y2 = map(int, p['bbox'])
                    cv2.rectangle(draw, (x1, y1), (x2, y2), color, 3)
                    cv2.putText(draw, f"ID:{p['id']} {status}", (x1, y1-10), 0, 0.7, color, 2)
                cv2.circle(draw, p['foot'], 6, (0, 0, 255), -1)
        view_frame = cv2.resize(draw, (960, 540))
        cv2.imshow("Dashboard", view_frame)
        if cv2.waitKey(1) == ord('q'): break
    
    is_running = False
    t.join()
    mqtt.close()
    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()