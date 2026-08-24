import torch
import torchvision
import sys
import platform
import os
import subprocess

def get_jetson_info():
    """젯슨 버전 정보 읽기"""
    try:
        with open("/etc/nv_tegra_release", "r") as f:
            return f.read().strip()
    except FileNotFoundError:
        return "Not a Jetson Device or version info not found"

def check_cudss():
    """cuDSS 라이브러리 설치 확인"""
    # 일반적인 설치 경로 확인
    paths = [
        "/usr/lib/aarch64-linux-gnu/libcudss.so",
        "/usr/local/cuda/lib64/libcudss.so"
    ]
    # ldconfig 캐시에서 검색
    try:
        result = subprocess.check_output("ldconfig -p | grep libcudss", shell=True).decode()
        if result:
            return f"Installed (Found in ldconfig): {result.strip()}"
    except subprocess.CalledProcessError:
        pass
        
    return "Not Found (libcudss check failed)"

def main():
    print("="*60)
    print(f"🚀 Jetson AI Environment Check Report")
    print("="*60)

    # 1. 하드웨어/OS 정보
    print(f"[1] System Info")
    print(f" - Python Version : {sys.version.split()[0]}")
    print(f" - OS Platform    : {platform.platform()}")
    print(f" - Jetson Version : {get_jetson_info()}")
    print("-" * 60)

    # 2. PyTorch 정보
    print(f"[2] PyTorch & AI Libs")
    print(f" - PyTorch Version     : {torch.__version__}")
    print(f" - TorchVision Version : {torchvision.__version__}")
    
    # 3. CUDA & GPU 연결 확인 (가장 중요!)
    cuda_avail = torch.cuda.is_available()
    print("-" * 60)
    print(f"[3] GPU Acceleration Check")
    print(f" - CUDA Available?     : {'✅ YES (Success!)' if cuda_avail else '❌ NO (Fail)'}")
    
    if cuda_avail:
        print(f" - CUDA Version (Torch): {torch.version.cuda}")
        print(f" - GPU Device Name     : {torch.cuda.get_device_name(0)}")
        print(f" - GPU Count           : {torch.cuda.device_count()}")
        
        # 간단한 텐서 연산 테스트
        try:
            x = torch.tensor([1.0, 2.0]).cuda()
            print(f" - Tensor Test         : ✅ Passed (Tensor loaded on GPU)")
        except Exception as e:
            print(f" - Tensor Test         : ❌ Failed ({e})")
    else:
        print("   >> GPU가 잡히지 않습니다. 설치 과정을 다시 확인하세요.")

    # 4. cuDSS 확인
    print("-" * 60)
    print(f"[4] Extra Libraries")
    print(f" - cuDSS Status        : {check_cudss()}")
    
    print("="*60)
    print("✨ 이 정보를 팀원들과 공유하세요.")

if __name__ == "__main__":
    main()
