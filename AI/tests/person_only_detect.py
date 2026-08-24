import cv2
from ultralytics import YOLO
import time
import torch

# ==========================================
# 설정 (Settings)
# ==========================================
MODEL_PATH = 'yolov8n.pt'
VIDEO_SOURCE = 0   # 웹캠
CONF_THRESHOLD = 0.5 # 50% 이상 확실할 때만 탐지 (오탐 방지)

print("="*50)
print(f"🚀 Starting Person-Only Detection")
print(f"🎯 Target Class: Person (ID: 0)")
print("="*50)

# 1. 모델 로드
model = YOLO(MODEL_PATH)

# 2. 카메라 연결
cap = cv2.VideoCapture(VIDEO_SOURCE)
if not cap.isOpened():
    print("❌ Error: 카메라를 열 수 없습니다.")
    exit()

print("✅ Camera connected! Press 'q' to Quit.")

prev_time = 0

while True:
    ret, frame = cap.read()
    if not ret:
        break

    # =======================================================
    # 3. YOLO 추론 (핵심 변경 구간)
    # classes=[0] : 0번 클래스(Person)만 탐지해라!
    # conf=0.5    : 확신이 50% 이상일 때만 박스 쳐라!
    # =======================================================
    results = model.predict(frame, device=0, verbose=False, stream=True, 
                            classes=[0], conf=CONF_THRESHOLD)

    # 4. 결과 시각화
    for result in results:
        annotated_frame = result.plot()

        # (선택) 몇 명인지 세서 화면에 띄우기
        person_count = len(result.boxes)
        cv2.putText(annotated_frame, f"Person Count: {person_count}", (10, 70), 
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 255), 2)

    # 5. FPS 표시
    curr_time = time.time()
    fps = 1 / (curr_time - prev_time) if prev_time != 0 else 0
    prev_time = curr_time
    
    cv2.putText(annotated_frame, f"FPS: {fps:.1f}", (10, 30), 
                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

    cv2.imshow("Person Only Detection", annotated_frame)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
print("\n👋 프로그램 종료.")
