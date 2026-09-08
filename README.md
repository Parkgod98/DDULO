# 🚇 DDULO

Jetson Orin Nano에서 승강장 영상을 분석해 실시간 인원 변화를 계산하고, 혼잡도 데이터로 전달하는 Edge AI 프로젝트입니다.

제가 맡은 부분은 `AI/`의 사람 탐지·추적·카운팅 파이프라인입니다.

## 제가 맡은 부분

- YOLOv8 기반 사람 탐지
- TensorRT 기반 Jetson 추론 최적화
- ByteTracker 기반 객체 추적
- ROI 진입·이탈을 이용한 In / Out 카운팅
- MQTT를 통한 실시간 결과 전송
- 실제 영상에서 confidence · IoU · ROI 조건 조정

`Python` `YOLOv8` `TensorRT` `ByteTrack` `Jetson Orin Nano` `MQTT`

## 어떻게 동작하나

```text
Camera
  ↓
YOLOv8
  ↓
ByteTracker
  ↓
ROI 진입 / 이탈 판별
  ↓
In / Out Count
  ↓
MQTT
  ↓
혼잡도 분석 · Backend · Android App
```

단순히 프레임마다 사람 수를 세면 같은 사람을 여러 번 세기 쉽습니다. 그래서 객체 ID를 유지하고, 사람의 발 위치에 가까운 지점을 기준으로 ROI 경계를 넘는 순간을 이벤트로 판단했습니다.

## 개발하면서 고민한 것

### 서버가 아니라 Edge에서 먼저 처리

영상 전체를 계속 서버로 보내기보다 Jetson에서 사람 수를 계산하고 결과만 전송하는 구조를 선택했습니다. 후속 시스템에는 영상 대신 작은 JSON 데이터만 전달됩니다.

### 실제 환경에서 카운팅이 흔들리는 문제

원근과 가림 때문에 단순 Bounding Box 기준으로는 진입 판별이 흔들렸습니다. ROI와 Anchor Point 기준을 조정하고 이미지·영상 테스트 결과를 비교하면서 카운팅 로직을 보정했습니다.

AI 모듈의 세부 실행 방법과 테스트 구조는 [`AI/README.md`](AI/README.md)에 정리했습니다.

<details>
<summary>전체 시스템 구성</summary>

| 영역 | 역할 | 기술 |
|---|---|---|
| Edge AI | 탐지 · 추적 · 인원 계수 | YOLOv8, TensorRT, ByteTracker |
| Middleware | 데이터 수신 · 혼잡도 분석 | FastAPI, MQTT, Redis |
| Backend | 공공 데이터와 실시간 데이터 통합 | Spring Boot, Redis |
| App | 경로 검색 · 혼잡도 표시 | Kotlin, Jetpack Compose |

</details>

이전 루트 README는 [`docs/legacy/README_2026-09-08.md`](docs/legacy/README_2026-09-08.md)에 보관했습니다.
