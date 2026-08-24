import cv2
from ultralytics import YOLO
import time
import torch

# ==========================================
# 설정 (Settings)
# ==========================================
MODEL_PATH = 'yolov8n.pt'   # 사용할 모델 (나중에 .engine으로 바꿀 예정)
VIDEO_SOURCE = 0            # 0: USB 웹캠, 문자열 넣으면 동영상 파일 경로

print("="*50)
print(f"🚀 Starting Real-time Detection with {MODEL_PATH}")
print("="*50)

# 1. 모델 로드
model = YOLO(MODEL_PATH)

# 2. 카메라(웹캠) 연결
cap = cv2.VideoCapture(VIDEO_SOURCE)

# 카메라 설정 (해상도 등) - 필요시 주석 해제하여 조정
# cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
# cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

if not cap.isOpened():
    print(f"❌ Error: 카메라(Source: {VIDEO_SOURCE})를 열 수 없습니다.")
    exit()

print(f"✅ Camera connected! Press 'q' to Quit.")

# 3. 실시간 루프 (Loop)
prev_time = 0

while True:
    # 프레임 읽기
    ret, frame = cap.read()
    if not ret:
        print("❌ Error: 프레임을 받아올 수 없습니다. (영상 종료?)")
        break

    # 4. YOLO 추론 (Inference)
    # stream=True: 비디오용 메모리 최적화 옵션
    # device=0: GPU 사용 강제
    results = model.predict(frame, device=0, verbose=False, stream=True)

    # 5. 결과 시각화
    # results는 제너레이터이므로 for문으로 하나씩 꺼내서 처리
    for result in results:
        # result.plot(): YOLO가 알아서 박스 그리고 라벨 달아준 이미지를 반환
        annotated_frame = result.plot()

    # 6. FPS(초당 프레임) 계산 및 표시
    curr_time = time.time()
    fps = 1 / (curr_time - prev_time) if prev_time != 0 else 0
    prev_time = curr_time
    
    cv2.putText(annotated_frame, f"FPS: {fps:.1f}", (10, 30), 
                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

    # 화면에 띄우기
    cv2.imshow("YOLOv8 Real-time Detection (Press 'q' to exit)", annotated_frame)

    # 'q' 키를 누르면 종료
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# 종료 후 뒷정리
cap.release()
cv2.destroyAllWindows()
print("\n👋 프로그램이 종료되었습니다.")
