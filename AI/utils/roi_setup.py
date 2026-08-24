import cv2
import yaml
import numpy as np
import tkinter as tk
from tkinter import simpledialog

# ==========================================
# 설정
# ==========================================
CONFIG_FILE = 'config.yaml'
VIDEO_SOURCE = 0
WIDTH, HEIGHT = 1280, 720 

# ==========================================
# 전역 변수
# ==========================================
points = []
rois = {}
paused = False

def mouse_callback(event, x, y, flags, param):
    global points
    # 그리는 모드일 때만 점 찍기 허용
    if event == cv2.EVENT_LBUTTONDOWN:
        points.append((x, y))
    elif event == cv2.EVENT_RBUTTONDOWN:
        points = []

def get_region_name_gui():
    """터미널 대신 GUI 팝업창으로 이름을 입력받는 함수"""
    # tkinter 메인 윈도우 숨기기 (이거 안 하면 빈 창이 하나 더 뜸)
    root = tk.Tk()
    root.withdraw() 
    
    # 팝업창 띄우기 (항상 최상단에 뜨도록 설정)
    root.attributes('-topmost', True)
    
    name = simpledialog.askstring("구역 설정", "구역 이름을 입력하세요 (예: 1-1):", parent=root)
    
    root.destroy() # 사용 후 폐기
    return name

def save_to_yaml():
    data = {'ROI_SETTINGS': rois}
    try:
        with open(CONFIG_FILE, 'w') as f:
            yaml.dump(data, f, default_flow_style=False)
        print(f"💾 설정 파일 저장 완료: {CONFIG_FILE}")
        return True
    except Exception as e:
        print(f"❌ 저장 실패: {e}")
        return False

def main():
    global rois, points, paused

    cap = cv2.VideoCapture(VIDEO_SOURCE)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, WIDTH)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, HEIGHT)

    if not cap.isOpened():
        print("❌ 카메라 연결 불가")
        return

    cv2.namedWindow('ROI Setup', cv2.WINDOW_NORMAL)
    cv2.resizeWindow('ROI Setup', WIDTH, HEIGHT)
    cv2.setMouseCallback('ROI Setup', mouse_callback)

    # 초기 프레임
    ret, frame = cap.read()
    if not ret: return
    
    print("=== [GUI Mode Started] ===")

    while True:
        if not paused:
            ret, raw_frame = cap.read()
            if ret: frame = raw_frame
            else: cap.set(cv2.CAP_PROP_POS_FRAMES, 0)

        display_frame = frame.copy()

        # 1. 저장된 구역 그리기
        for name, pts in rois.items():
            pts_np = np.array(pts, np.int32).reshape((-1, 1, 2))
            cv2.polylines(display_frame, [pts_np], True, (0, 255, 0), 2)
            # 이름 표시 (배경 깔아서 잘 보이게)
            text_pos = tuple(pts[0])
# (변경 전)
            # cv2.putText(display_frame, name, text_pos, cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 4)
            # cv2.putText(display_frame, name, text_pos, cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)

            # (변경 후) 맨 뒤에 cv2.LINE_AA 추가 ★
            cv2.putText(display_frame, name, text_pos, cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 4, cv2.LINE_AA)
            cv2.putText(display_frame, name, text_pos, cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2, cv2.LINE_AA)
        # 2. 현재 그리는 중
        if len(points) > 0:
            pts_np = np.array(points, np.int32)
            if len(points) > 1:
                cv2.polylines(display_frame, [pts_np], False, (0, 0, 255), 2)
            for pt in points:
                cv2.circle(display_frame, pt, 5, (0, 0, 255), -1)

        # 안내 문구 오버레이
        info_text = "SPACE: Pause/Resume | Click: Points | 'n': Save Region | 's': Save File"
        cv2.rectangle(display_frame, (0, 0), (WIDTH, 40), (0, 0, 0), -1) # 상단 검은띠
# (변경 전) 두께 1, 기본 라인 타입
        # cv2.putText(display_frame, info_text, (10, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)

        # (변경 후) 두께 2로 키우고, cv2.LINE_AA(부드럽게) 추가 ★
        cv2.putText(display_frame, info_text, (10, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2, cv2.LINE_AA)

        if paused:
             cv2.putText(display_frame, "PAUSED", (WIDTH//2 - 100, HEIGHT//2), cv2.FONT_HERSHEY_SIMPLEX, 2, (0, 0, 255), 3)

        cv2.imshow('ROI Setup', display_frame)
        key = cv2.waitKey(30) & 0xFF

        # [Space] 멈춤/재생
        if key == 32:
            paused = not paused
        
        # [N] 구역 이름 입력 (GUI 팝업)
        elif key == ord('n'):
            if len(points) < 3:
                # 경고창 띄우기에는 과하니 콘솔/화면 로그 정도만
                print("⚠️ 점을 3개 이상 찍어야 합니다.")
            else:
                # ★ 여기가 핵심 변경 포인트 ★
                paused = True # 입력하는 동안 화면 멈춤
                area_name = get_region_name_gui() # 팝업창 호출
                
                if area_name: # 취소 안 하고 입력했으면
                    rois[area_name] = list(points)
                    points = []
                    print(f"✅ 구역 추가됨: {area_name}")
                else:
                    print("❌ 입력 취소됨")
                
                # 입력 끝나면 다시 재생 (원하면 paused=True 유지해도 됨)
                paused = False 

        # [S] 저장
        elif key == ord('s'):
            if save_to_yaml():
                # 저장 완료 알림창 (선택사항)
                top = tk.Tk()
                top.withdraw()
                top.attributes('-topmost', True) # 맨 위에
                from tkinter import messagebox
                messagebox.showinfo("Success", "설정 파일(config.yaml)이 저장되었습니다.")
                top.destroy()
                break

        elif key == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()
