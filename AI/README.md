# AI 기반 실시간 객체 인식 · 카운팅 시스템

Jetson Orin Nano 환경에서 YOLOv8 + TensorRT 기반으로 동작하는 실시간 객체 인식 및 사람 카운팅 시스템입니다.  
영상/이미지 입력을 기반으로 ROI 진입 판별, 객체 추적(ByteTracker), 실시간 카운팅 및 MQTT 전송을 수행합니다.  
성능 최적화(conf=0.25, iou=0.5)와 보안 MQTT 통신이 적용되어 있으며, 실환경 배포를 고려한 구조로 설계되었습니다.

---

### 개발환경

#### Hardware
- NVIDIA Jetson Orin Nano

#### OS / Runtime
- Ubuntu (Jetson 기본 OS)
- Python 3.8+

#### AI / Vision
- YOLOv8 (n, s, m, l, x)
- TensorRT
- OpenCV
- ByteTracker (YOLO 내장 Tracker)

#### Messaging / Network
- MQTT (Port: 8000)
- TLS 기반 보안 통신 적용

#### 기타
- PyTorch (Jetson 환경 빌드)
- CUDA / cuDNN
- requirements.txt 제공

---

### Install

```bash
# 가상환경 (선택)
python3 -m venv venv
source venv/bin/activate

# 필수 패키지 설치
pip install -r requirements.txt
```

#### TensorRT 엔진 준비

* `models/` 폴더에 엔진 파일 포함

  * yolov8n.engine
  * yolov8m.engine

엔진이 없는 경우:

```bash
python export_trt.py
```

---

### Usage

#### 1. ROI 설정 (GUI)

```bash
python roi_gui.py
```

* ROI 영역 설정 후 YAML 파일로 저장
* 다양한 해상도 대응 가능

#### 2. 실시간 추론 (Webcam / Video)

```bash
python main.py
```

기능:

* 실시간 객체 인식 (Person Class 필터링)
* ByteTracker 기반 추적
* Anchor Point(foot 기준) 기반 ROI 진입 판별
* In / Out 카운팅
* OSD Overlay (ROI, Count, FPS)

#### 3. 이미지 / 영상 테스트

```bash
python tests/img_test.py
```

* 입력: `tests/imgInput/`
* 출력: `tests/imgOutput/`
* 요약 CSV: `tests/imgCSVs/`

---

### 주요 설정값

| 항목              | 값           |
| --------------- | ----------- |
| conf_threshold  | 0.25        |
| iou_threshold   | 0.5         |
| Tracker         | ByteTracker |
| Detection Class | Person      |

(conf, iou 값은 이미지·영상 테스트를 통해 최적화됨)

---

### 외부 서비스 정보

#### MQTT

* 역할: 실시간 카운팅 결과 전송
* 포트: `8000`
* 보안: TLS 적용
* 데이터 포맷: JSON 직렬화

```json
{
  "timestamp": "...",
  "camera_id": "...",
  "in_count": 12,
  "out_count": 9,
  "current_count": 3
}
```

---

### DB / 데이터 구조

* 별도 DB 미사용
* 카운팅 결과는 MQTT 메시지(JSON) 형태로 외부 시스템 연동
* ROI 설정 정보는 YAML 파일로 관리
* 테스트 결과는 CSV 파일로 저장

---

### 시연 시나리오

1. ROI GUI 실행 → 관심 영역 설정
2. Webcam 또는 영상 입력으로 main.py 실행
3. 객체 인식 및 추적 시작
4. ROI 진입 시 In / Out 판별
5. 화면에 실시간 카운트 및 FPS 표시
6. MQTT 통해 카운팅 결과 전송
7. 이미지/영상 테스트 시 CSV 결과 확인

---

### 디렉토리 구조 (AI)

```
AI/
 ├─ main.py
 ├─ detector.py
 ├─ export_trt.py
 ├─ models/
 │   ├─ yolov8n.engine
 │   └─ yolov8m.engine
 ├─ tests/
 │   ├─ imgInput/
 │   ├─ imgOutput/
 │   ├─ imgCSVs/
 │   └─ img_test.py
 ├─ roi/
 │   └─ roi.yaml
 └─ requirements.txt
```

---

### Notes

* IDE 설정 파일(.idea, workspace.xml 등)은 git에서 제외
* 테스트 데이터는 외부 링크로 관리
* conf / iou 변경 시 detector.py 및 main.py 반영 필요

```

