from ultralytics import YOLO

# 1. 내 모델 로드 (yolov8m.pt)
print("Load Custom Model: yolov8m.pt...")
model = YOLO("yolov8m.pt")

# 2. TensorRT 엔진으로 변환 (Export)
# format="engine": TensorRT 포맷
# half=True      : FP16 반정밀도 (Jetson 속도 핵심!)
# device=0       : GPU 사용
print("🚀 Exporting model to TensorRT format... (This may take 5-10 mins)")

# 결과물은 자동으로 'yolov8m.engine'으로 저장됩니다.
model.export(format="engine", half=True, device=0)

print("✅ Export Completed! 'yolov8m.engine' file is created.")