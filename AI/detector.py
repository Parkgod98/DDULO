import cv2
from ultralytics import YOLO

class Detector:
    def __init__(self, model_path='yolov8n_best.engine', conf_threshold=0.5):
        """
        Detector 클래스 초기화
        :param model_path: .pt 또는 .engine 파일 경로
        :param conf_threshold: 탐지 신뢰도 임계값 (기본 0.5)
        """
        print(f"🔄 모델 로딩 중... ({model_path})")
        # task='detect' 명시
        self.model = YOLO(model_path, task='detect')
        self.conf = conf_threshold
        self.classes = [0] # 0: Person (사람만 탐지하도록 고정)
        print("✅ 모델 로드 완료!")

    def detect_frame(self, frame):
        """
        프레임을 받아 객체를 탐지하고 결과를 반환
        :param frame: OpenCV 영상 프레임
        :return: (결과 이미지, 객체 정보 리스트)
                 객체 정보 리스트: [{'bbox': [x1, y1, x2, y2], 'conf': 0.85, 'class': 0}, ...]
        """
        # =========================================================
        # ★ 수정 포인트: classes=[0] (사람만), stream=True (메모리 최적화)
        # =========================================================
        results = self.model(frame, 
                           conf=self.conf, 
                           classes=self.classes, 
                           iou=0.65,          # <--- ★ 여기가 핵심입니다 (기본 0.45 -> 0.6)
                           verbose=False, 
                           stream=True)
        
        detections = []
        result_frame = frame.copy() 

        # stream=True일 때는 루프를 돌려야 결과가 나옵니다.
        for result in results:
            # 1. 박스 좌표 추출
            boxes = result.boxes
            for box in boxes:
                x1, y1, x2, y2 = map(int, box.xyxy[0])
                confidence = float(box.conf[0])
                cls = int(box.cls[0])

                # 2. 반환할 리스트에 담기
                detection_info = {
                    'bbox': [x1, y1, x2, y2],
                    'conf': round(confidence, 2),
                    'class': cls
                }
                detections.append(detection_info)

                # 3. 화면에 그리기 (초록색 박스 + 라벨)
                label = f"Person {confidence:.2f}"
                cv2.rectangle(result_frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
                cv2.putText(result_frame, label, (x1, y1 - 10), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
            
        return result_frame, detections