import cv2
from detector import Detector

# roi_manager, yaml 관련 임포트 다 제거함

def run_bbox_only():
    print("🚀 [Simple Mode] 1.png BBox 그리기 시작")
    
    # 1. Detector만 로드 (모델 경로 확인해주세요)
    det = Detector(model_path='models/yolov8n_best.engine', conf_threshold=0.4)

    # 2. 이미지 불러오기
    image_path = '1.png'
    frame = cv2.imread(image_path)

    if frame is None:
        print(f"❌ 에러: {image_path} 파일이 안 보입니다.")
        return

    # 3. 추론 수행
    # detections 리스트에 결과가 담겨옵니다.
    _, detections = det.detect_frame(frame)
    print(f"👉 감지된 사람 수: {len(detections)}명")

    # 4. BBox만 그리기 (다른 거 일절 없음)
    for info in detections:
        x1, y1, x2, y2 = info['bbox']
        # 초록색 네모 그리기 (색상 BGR: (0, 255, 0), 두께: 2)
        cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)

    # 5. 저장
    save_path = 'result_bbox.png'
    cv2.imwrite(save_path, frame)
    
    print(f"✅ 완료! '{save_path}' 확인해보세요.")

if __name__ == "__main__":
    run_bbox_only()
