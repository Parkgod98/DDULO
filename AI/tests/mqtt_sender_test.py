import cv2
import time
import json
from detector import Detector
from roi_manager import ROIManager

def main():
    print("🎥 [Real Test] 실시간 탐지 데이터기반 JSON 스키마 검증")
    print("-------------------------------------------------------")
    
    # 1. 실제 엔진 로드
    roi_man = ROIManager(config_file='config.yaml')
    det = Detector(model_path='yolov8n_best.engine', conf_threshold=0.4)

    # 2. 카메라 설정
    cap = cv2.VideoCapture(0)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

    if not cap.isOpened():
        print("❌ 카메라 연결 실패")
        return

    print("✅ 카메라 가동. 실제 데이터를 JSON으로 변환합니다.")
    print("   (터미널 로그를 확인하세요)")

    # 과도한 로그 방지를 위한 타이머
    last_print_time = time.time()

    while True:
        ret, frame = cap.read()
        if not ret: break

        # ------------------------------------------------
        # 1. 실제 탐지 및 카운팅 (Ticket 48, 49 로직)
        # ------------------------------------------------
        _, detections = det.detect_frame(frame)
        
        zone_counts = {name: 0 for name in roi_man.rois.keys()}
        
        for info in detections:
            bbox = info['bbox']
            zone_name, _ = roi_man.check_entry(bbox)
            if zone_name:
                zone_counts[zone_name] += 1

        # ------------------------------------------------
        # 2. JSON 스키마 적용 및 직렬화 (Ticket 50 핵심)
        # ------------------------------------------------
        # 1초에 한 번씩만 출력 (너무 빠르면 눈 아픔)
        if time.time() - last_print_time > 1.0:
            current_time = time.strftime('%Y-%m-%d %H:%M:%S')
            
            # [스키마 정의]
            payload = {
                "timestamp": current_time,
                "data": zone_counts
            }
            
            # [직렬화]
            try:
                json_output = json.dumps(payload, ensure_ascii=False)
                print(f"📡 [Real JSON]: {json_output}")
            except Exception as e:
                print(f"❌ JSON 변환 에러: {e}")

            last_print_time = time.time()

        # ------------------------------------------------
        # 3. 화면 표시 (검증용)
        # ------------------------------------------------
        # 화면에도 박스를 그려서, JSON 숫자랑 맞는지 비교
        frame = roi_man.draw_rois(frame, zone_counts)
        for info in detections:
            x1, y1, x2, y2 = info['bbox']
            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
            
        cv2.imshow("Real Data Schema Test", frame)

        if cv2.waitKey(1) == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()
