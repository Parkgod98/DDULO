# 🚇 뚫어 (DDULO)

**Jetson Orin Nano 기반 실시간 지하철 혼잡도 분석 및 경로 추천 시스템**

서울시 공공 데이터와 Edge AI 객체 인식을 결합해 승강장 혼잡도를 실시간으로 계산하고, 이를 서비스의 경로 추천까지 연결한 팀 프로젝트입니다.

## 프로젝트 핵심

```text
Camera
  ↓
YOLOv8 + TensorRT
  ↓
ByteTracker + ROI 판별
  ↓
실시간 In / Out Count
  ↓
MQTT
  ↓
Redis / FastAPI
  ↓
Spring Boot
  ↓
Android App
```

단순 객체 탐지에서 끝내지 않고 **Edge Device에서 생성한 실시간 데이터를 서비스 계층까지 전달하는 전체 파이프라인**을 구현했습니다.

## My Contribution — Edge AI

제가 담당한 핵심 영역은 `AI/`의 실시간 인원 탐지·추적·카운팅 파이프라인입니다.

- Jetson Orin Nano 기반 YOLOv8 실시간 추론 환경 구성
- TensorRT 엔진을 활용한 Edge 추론 최적화
- ByteTracker 기반 동일 객체 추적
- 사람의 발 위치를 Anchor Point로 사용한 ROI 진입 판별
- ROI 진입/이탈에 따른 In / Out 인원 계수
- MQTT를 이용한 실시간 카운팅 데이터 전송
- 이미지·영상 테스트를 통한 `confidence`, `IoU` 파라미터 검증
- 원근과 가림이 있는 실제 환경을 고려한 카운팅 로직 보정

AI 모듈의 상세 구현은 [`AI/README.md`](AI/README.md)에서 확인할 수 있습니다.

## 왜 Edge AI로 처리했나

실시간 혼잡도 계산은 카메라 영상을 중앙 서버로 계속 전송하는 대신, 현장에서 사람 수를 계산한 뒤 필요한 결과만 전달하는 구조로 설계했습니다.

이를 통해 영상 자체가 아닌 **카운팅 결과 중심의 경량 데이터**를 후속 시스템으로 전달할 수 있고, Jetson 환경에서 추론부터 추적까지 처리하는 구조를 검증할 수 있었습니다.

## AI 파이프라인

### 1. Detection

YOLOv8에서 Person class를 중심으로 객체를 탐지합니다.

### 2. Tracking

ByteTracker를 사용해 프레임 사이의 객체 ID를 유지하고 동일 인물을 중복 카운팅하지 않도록 했습니다.

### 3. ROI Counting

Bounding Box 중심이 아니라 사람의 발 위치에 가까운 Anchor Point를 기준으로 ROI 진입 여부를 판별합니다.

```text
Detection
   ↓
Tracking ID 유지
   ↓
Anchor Point 계산
   ↓
ROI 내부 / 외부 상태 비교
   ↓
IN / OUT 이벤트 생성
```

### 4. Messaging

카운팅 결과는 JSON 형태로 MQTT를 통해 전달합니다.

```json
{
  "timestamp": "...",
  "camera_id": "...",
  "in_count": 12,
  "out_count": 9,
  "current_count": 3
}
```

## 전체 시스템 구성

| 영역 | 역할 | 기술 |
|---|---|---|
| Edge AI | 사람 탐지 · 추적 · 인원 계수 | YOLOv8, TensorRT, ByteTracker, Jetson Orin Nano |
| Middleware | 데이터 수신 · 검증 · 혼잡도 분석 | FastAPI, MQTT, Redis |
| Backend | 공공 데이터와 실시간 혼잡도 통합 | Spring Boot, RDB, Redis |
| App | 경로 검색 · 혼잡도 · 탑승 추천 | Kotlin, Jetpack Compose |

## 저장소 구조

```text
DDULO/
├─ AI/                    # 제가 담당한 Edge AI 모듈
├─ Congestion_Algorithm/  # 혼잡도 분석
├─ Redis/                 # MQTT / Redis Middleware
├─ ddulo-api/             # Spring Boot Backend
├─ DDULO/                 # Android App
├─ exec/
└─ mosquitto/
```

## 기술 스택

`Python` `YOLOv8` `TensorRT` `OpenCV` `ByteTracker` `Jetson Orin Nano` `MQTT` `FastAPI` `Redis` `Spring Boot` `Kotlin`

## AI 모듈 실행

```bash
cd AI
pip install -r requirements.txt
python main.py
```

세부 환경 설정과 테스트 방법은 [`AI/README.md`](AI/README.md)를 참고하세요.

기존 루트 README는 개발 과정 보존을 위해 [`docs/legacy/README_2026-09-08.md`](docs/legacy/README_2026-09-08.md)에 남겨두었습니다.
