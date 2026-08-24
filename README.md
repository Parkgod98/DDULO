
# 🚇 뚫어(Ddulo) : 지하철 실시간 혼잡도 및 경로 예측 시스템

**"막힌 길을 뚫어주다"** 서울시 공공 데이터와 실시간 Edge AI 객체 인식을 결합하여, 지하철 승강장 및 열차 내 혼잡도를 분석하고 최적의 이동 경로를 제안하는 통합 솔루션입니다.

---

## 📌 Project Overview

본 프로젝트는 단순한 경로 탐색을 넘어, **실시간 물리적 혼잡도(IoT)**와 **통계 기반 예측 데이터**를 융합하여 사용자에게 가장 쾌적한 지하철 이용 경험을 제공합니다.

* **실시간 객체 인식:** Jetson Orin Nano를 활용한 승강장 내 실시간 인원 카운팅
* **데이터 파이프라인:** MQTT(TLS)를 통한 보안 데이터 전송 및 Redis 캐싱 기반의 초고속 데이터 조회
* **경로 예측 알고리즘:** 실시간/과거 데이터를 분석하여 혼잡도를 고려한 최단·추천 경로 API 제공

---

## 🏗 System Architecture

프로젝트는 크게 세 가지 레이어로 구성되어 유기적으로 동작합니다.

### 1. [AI Edge Device (Jetson)](https://lab.ssafy.com/s14-webmobile3-sub1/S14P11A204/-/tree/main/AI?ref_type=heads)

승강장에 설치된 카메라를 통해 실시간으로 사람을 감지하고 카운팅합니다.

* **Core:** YOLOv8 + TensorRT (Jetson Orin Nano 최적화)
* **Tracking:** ByteTracker 기반 객체 추적 및 ROI 진입 판별
* **Output:** 실시간 In/Out 데이터 생성 및 MQTT 전송

### 2. [IoT Middleware & Analysis](https://lab.ssafy.com/s14-webmobile3-sub1/S14P11A204/-/tree/main/Redis?ref_type=heads#ddulo-iot-middleware--congestion-analysis)

Edge Device로부터 전송된 데이터를 가공하고 캐싱하는 미들웨어입니다.

* **Communication:** Paho-MQTT (SSL/TLS 보안 적용)
* **Processing:** FastAPI 기반 데이터 검증 및 혼잡도 분석 알고리즘 적용
* **Storage:** 분석 결과를 Redis에 저장하여 Backend 부하 감소 및 응답 속도 최적화

### 3. [Backend & Service](https://lab.ssafy.com/s14-webmobile3-sub1/S14P11A204/-/tree/main/ddulo-api?ref_type=heads#%EC%A7%80%ED%95%98%EC%B2%A0-%EC%8B%A4%EC%8B%9C%EA%B0%84-%ED%98%BC%EC%9E%A1%EB%8F%84-%EA%B8%B0%EB%B0%98-%EA%B2%BD%EB%A1%9C-%EC%98%88%EC%B8%A1-%EC%8B%9C%EC%8A%A4%ED%85%9C-backend--iot)

사용자에게 최종 서비스를 제공하는 메인 컨트롤러입니다.

* **Core:** Spring Boot (JDK 21)
* **Data:** 공공 API(열차 도착 정보) + Redis(실시간 혼잡도) + RDB 통합
* **Service:** 역 상세 조회, 혼잡도 기반 경로 예측 API 제공

### 4. [Frontend (Android App)](https://lab.ssafy.com/s14-webmobile3-sub1/S14P11A204/-/tree/main/DDULO?ref_type=heads)

사용자 위치 및 입력을 기반으로 서비스 기능을 모바일 화면으로 제공합니다

* **Core** : Kotlin 2.1.x, Jetpack Compose (Material3), MVVM + State Hoisting
* **Architecture** : MVVM + State Hoisting
* **Service** : 경로 검색, 경로 상세, 실시간 혼잡도 & 탑승 추천, 즐겨찾기 뷰 제공

---

## 📂 Directory Structure

```bash
.
├── 📁 AI            # Edge AI (YOLOv8, TensorRT, ROI 설정 GUI)
├── 📁 Congestion_Algorithm # 혼잡도 분석 알고리즘  
├── 📁 Redis         # IoT Middleware (FastAPI, MQTT Subscriber, Redis Logic)
├── 📁 ddulo-api       # Backend (Spring Boot, Data Pipeline)
├── 📁 DDULO         # Frontend(Native Android)
├── 📁 exec          # 실행 파일    
├── 📁 mosquitto     # MQTT Broker (Mosquitto)
└── 📁 README.md     # Root README

```

> 각 디렉토리를 클릭하면 해당 모듈의 **상세 설치 및 실행 방법**을 확인할 수 있습니다.

---

## 🚀 Quick Start (Summary)

전체 시스템 기동을 위한 핵심 요약입니다. (상세 내용은 각 폴더 참조)

1. **Infrastructure:** Redis 및 MQTT Broker(Mosquitto) 실행 확인
2. **AI:** `python main.py` (Jetson 환경에서 실시간 추론 시작)
3. **Middleware:** `uvicorn api_server:app` (데이터 수신 및 캐싱 서버 시작)
4. **Backend:** Spring Boot 애플리케이션 실행

---

## 🛠 Tech Stack

* **Language:** Java(JDK 21), Kotlin, Python 3.9+, SQL
* **Framework:** Spring Boot 3.5.x, FastAPI, PyTorch
* **Infrastructure:** Docker, Docker Composer(Redis, Mosquitto(MQTT), RDB, Nginx)
* **Hardware:** NVIDIA Jetson Orin Nano, Raspberry Pi (Gateway)
* **CI/CD:** GitLab CI/CD

---
