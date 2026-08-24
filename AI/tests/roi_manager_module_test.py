import cv2
from detector import Detector
from roi_manager import ROIManager

def main():
    print("🔢 [Task 48] 구역별 인원 카운팅(Counting) 테스트")
    print("-------------------------------------------------")
    
    # 1. 매니저 & 탐지기 로드
    roi_man = ROIManager(config_file='config.yaml')
    det = Detector(model_path='yolov8n_best.engine', conf_threshold=0.5)

    # 2. 해상도 1280x720 (형님 설정파일 좌표 대응)
    cap = cv2.VideoCapture(0)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

    if not cap.isOpened():
        print("❌ 카메라 연결 실패")
        return

    # 창 크기 조절 가능
    window_name = "ROI Counting Test"
    cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(window_name, 1280, 720)

    while True:
        ret, frame = cap.read()
        if not ret: break

        # ---------------------------------------------------------
        # 1. 사람 탐지
        # ---------------------------------------------------------
        # 프레임 원본으로 탐지 수행
        _, detections = det.detect_frame(frame)
        
        # ---------------------------------------------------------
        # 2. 카운팅 로직 (여기가 핵심!)
        # ---------------------------------------------------------
        # 구역별 카운터 초기화 (예: {'1-1': 0, '1-2': 0, ...})
        zone_counts = {name: 0 for name in roi_man.rois.keys()}

        for info in detections:
            bbox = info['bbox']
            
            # 이 사람이 어느 구역에 있는지 확인
            zone_name, _ = roi_man.check_entry(bbox)
            
            # 구역 안에 있으면 해당 구역 카운트 +1
            if zone_name:
                zone_counts[zone_name] += 1

        # ---------------------------------------------------------
        # 3. 결과 그리기 (draw_rois에 카운트 정보 전달)
        # ---------------------------------------------------------
        # 이제 draw_rois가 "1-1 : 3" 처럼 숫자를 그려줍니다.
        result_frame = roi_man.draw_rois(frame, zone_counts=zone_counts)

        # (선택사항) 사람 박스도 보고 싶으면 그림
        for info in detections:
            x1, y1, x2, y2 = info['bbox']
            cv2.rectangle(result_frame, (x1, y1), (x2, y2), (0, 255, 0), 2)

        cv2.imshow(window_name, result_frame)

        if cv2.waitKey(1) == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()