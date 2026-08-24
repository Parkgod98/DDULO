# Ddulo IoT Middleware & Congestion Analysis

**Ddulo(뚜로)** 서비스의 **IoT 데이터 수신 및 혼잡도 분석 미들웨어**입니다.

Edge Device(Jetson)로부터 전송된 승강장 데이터를 **MQTT(SSL/TLS)** 프로토콜을 통해 수신하고, `Congestion_Algorithm` 모듈을 통해 분석한 뒤, 그 결과를 **Redis**에 캐싱하여 메인 백엔드(`ddulo-api`, Spring Boot)가 실시간으로 사용할 수 있도록 제공합니다.

### 주요 기능

* **Secure MQTT Communication**: `paho-mqtt`를 사용하여 SSL/TLS 인증서 기반의 보안 통신 구현 (Self-signed Certificate 지원).
* **Real-time Data Processing**: `Congestion_Algorithm`을 import하여 실시간 영상 분석 데이터를 가공.
* **Redis Caching**: 분석된 혼잡도 데이터를 Redis에 저장하여 프론트엔드/백엔드에서의 조회 속도 최적화.
* **FastAPI Server**: 데이터 수신 및 모니터링을 위한 경량화된 Python 서버 구동.

### 개발환경

* **Language**: Python 3.9+
* **Framework**: FastAPI, Uvicorn
* **Key Libraries**:
* `redis == 5.0.1`: 데이터 캐싱 및 관리
* `paho-mqtt == 1.6.1`: MQTT 브로커 통신 (SSL 지원)
* `python-dotenv == 1.0.0`: 환경 변수 관리
* `pandas`: 데이터 분석 및 처리


* **Infrastructure**:
* Mosquitto Broker (Port 8000, SSL/TLS Enabled)
* Redis Server



### Install

프로젝트 실행을 위해 필요한 의존성 패키지를 설치합니다.

```bash
python -m venv venv
# 가상환경 진입 (선택사항)
source venv/bin/activate

# 패키지 설치
pip install -r requirements.txt

```

#### ⚠️ 인증서 파일 배치 (필수)

MQTT 통신 시 SSL 인증이 필요하므로, 아래 경로에 `ca.crt` 파일이 반드시 존재해야 합니다.

* 경로: `/home/a204/home/ddulo/S14P11A204/mosquitto/config/certs/ca.crt`
* *Note: 로컬 개발 시에는 해당 경로를 `.env`나 코드에서 환경에 맞게 수정해야 할 수 있습니다.*

### Usage

#### 1. 환경 변수 설정 (.env)

프로젝트 루트(또는 실행 폴더)에 `.env` 파일을 생성하고 아래 변수들을 설정합니다.

```ini
# MQTT 설정
MQTT_HOST=i14a204.p.ssafy.io
MQTT_PORT=8000
MQTT_TOPIC=/platform   # 또는 # (전체 구독)

# Redis 설정
REDIS_HOST=localhost
REDIS_PORT=6379
# REDIS_PASSWORD=  (필요시 설정)

```

#### 2. 서버 실행

FastAPI 서버(Uvicorn)를 통해 IoT 수신 및 로직을 가동합니다.

```bash
# Redis 폴더 내부의 api_server.py가 메인 엔트리 포인트인 경우
cd Redis
uvicorn api_server:app --reload --host 0.0.0.0 --port 8000

```

* 서버가 시작되면 **MQTT Broker와 자동으로 연결**을 시도하며, 터미널 로그를 통해 `✅ [SDK] MQTT 연결 성공` 메시지를 확인할 수 있습니다.
* 수신된 데이터는 실시간으로 Redis에 업데이트됩니다.
"""