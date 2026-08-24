import time
import os
import cv2
import numpy as np
from ultralytics import YOLO

# ==========================================
# 1. 파일 경로 설정 (본인 파일명에 맞게 수정)
# ==========================================
PT_MODEL_PATH = "yolov8n_best.pt"      # 원본 모델
ENGINE_MODEL_PATH = "yolov8n_best.engine"  # 변환된 엔진 모델
TEST_FRAMES = 100  # 성능 측정할 프레임 수 (많을수록 정확함)

def benchmark_model(model_path, dummy_input, label):
    """모델 로드 및 FPS 측정 함수"""
    print(f"\n--- [{label}] 측정 시작 ({model_path}) ---")
    
    # 1. 모델 파일 존재 확인
    if not os.path.exists(model_path):
        print(f"❌ 오류: 파일을 찾을 수 없습니다 -> {model_path}")
        return None

    try:
        # 2. 모델 로드
        model = YOLO(model_path, task='detect')
        
        # 3. 웜업 (Warm-up): 초기 로딩 시간 제외
        print("🔥 웜업 중 (Warm-up)...")
        for _ in range(10):
            model(dummy_input, verbose=False)
            
        # 4. 실제 속도 측정
        print(f"⏱️ {TEST_FRAMES} 프레임 추론 속도 측정 중...")
        start_time = time.time()
        
        for _ in range(TEST_FRAMES):
            model(dummy_input, verbose=False)
            
        end_time = time.time()
        
        # 5. 결과 계산
        duration = end_time - start_time
        fps = TEST_FRAMES / duration
        print(f"✅ [{label}] 결과: {fps:.2f} FPS (총 {duration:.2f}초 소요)")
        return fps
        
    except Exception as e:
        print(f"❌ 실행 중 오류 발생: {e}")
        return None

def main():
    print("=============================================")
    print("🚀 [Perf] TensorRT 변환 전후 성능 비교 테스트")
    print("=============================================")

    # 테스트용 더미 이미지 생성 (검은 화면, 640x640)
    # 실제 카메라 I/O 딜레이를 배제하고 '순수 모델 추론 속도'만 측정하기 위함
    dummy_img = np.zeros((640, 640, 3), dtype=np.uint8)

    # 1. 기존 PyTorch 모델 (.pt) 측정
    pt_fps = benchmark_model(PT_MODEL_PATH, dummy_img, "PyTorch (.pt)")
    
    # 2. TensorRT 엔진 모델 (.engine) 측정
    engine_fps = benchmark_model(ENGINE_MODEL_PATH, dummy_img, "TensorRT (.engine)")

    # 3. 최종 결과 리포트
    print("\n=============================================")
    print("📊 최종 성능 비교 리포트")
    print("=============================================")
    
    if pt_fps and engine_fps:
        improvement = engine_fps / pt_fps
        diff = engine_fps - pt_fps
        
        print(f"🔹 PyTorch  (.pt)     : {pt_fps:.2f} FPS")
        print(f"🔹 TensorRT (.engine) : {engine_fps:.2f} FPS")
        print("---------------------------------------------")
        print(f"📈 성능 향상 : {diff:+.2f} FPS 증가")
        print(f"⚡ 배율      : 약 {improvement:.1f}배 빨라짐")
        
        if improvement >= 1.2: # 1.2배 이상 빨라졌으면 성공으로 간주
            print("\n✅ [SUCCESS] TensorRT 최적화 성공! (완료 조건 충족)")
        else:
            print("\n⚠️ [WARNING] 성능 차이가 크지 않습니다. 변환 옵션(fp16 등)을 확인하세요.")
    else:
        print("\n❌ [FAIL] 테스트 실패 (파일 경로를 확인해주세요)")

if __name__ == "__main__":
    main()
