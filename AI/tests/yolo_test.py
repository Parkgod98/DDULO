from ultralytics import YOLO
import torch
import time

print("="*60)
print("🚀 [Env Check] YOLOv8 Object Detection Test")
print("="*60)

# 1. GPU 연결 확인 (가장 중요)
if torch.cuda.is_available():
    print(f"✅ GPU State   : On (Device: {torch.cuda.get_device_name(0)})")
else:
    print("❌ GPU State   : Off (CPU로 돌아갑니다. 환경 설정을 확인하세요!)")
    # CPU로라도 돌아가는지 보려면 아래 줄 주석 처리하지 마세요.
    # exit() 

# 2. 모델 로드 (인터넷 연결 필요 - 최초 1회 다운로드)
print("\n[Step 1] Loading Model...")
model = YOLO("yolov8n.pt")  # 가장 가벼운 nano 버전
print(" -> Model Loaded Successfully!")

# 3. 추론 실행
print("\n[Step 2] Running Inference on 'bus.jpg'...")
start_time = time.time()

# device=0 은 GPU 0번을 강제로 쓰겠다는 뜻
results = model.predict("https://ultralytics.com/images/bus.jpg", save=True, device=0, verbose=False)

end_time = time.time()
print(f" -> Inference Time : {end_time - start_time:.4f} seconds")

# 4. 결과 안내
save_path = results[0].save_dir
print("\n[Step 3] Result")
print(f"✅ 테스트 완료! 결과 파일이 아래 위치에 저장되었습니다.")
print(f"📂 경로: {save_path}")
print("="*60)
