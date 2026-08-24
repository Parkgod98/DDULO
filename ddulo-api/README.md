# 지하철 실시간 혼잡도 기반 경로 예측 시스템 (Backend & IoT)

서울시 지하철 데이터를 기반으로 **실시간·과거 혼잡도**, **열차 도착 정보**, **최단/추천 경로**를 통합 제공하는 시스템입니다.  
Backend(Spring Boot)와 IoT·Python(FastAPI) 기반 혼잡도 계산 서버가 연동되며, Redis·RDB를 활용해 대용량 데이터를 효율적으로 처리합니다.

혼잡도 알고리즘, 실시간 전광판 데이터, 경로 예측 결과를 하나의 파이프라인으로 묶어 **최종 화면에 필요한 데이터를 단일 API로 제공**하는 것이 핵심 목표입니다.

---

### 개발환경
- Kotlin 2.1.x
- Spring Boot
- JDK 21 (eclipse-temurin)
- Python 3.x
- FastAPI
- Redis
- RDB (JPA 기반)
- MQTT (IoT 실시간 데이터 수집)
- Docker / Docker Compose
- GitLab CI/CD

---

### Install
1. 저장소 클론
   ```bash
   git clone <repository-url>
    ```

2. Backend 환경 설정

   * `application.yml` 또는 `application.properties` 설정
   * Redis, RDB 연결 정보 입력
3. Python(FastAPI) 서버 설정

   ```bash
   pip install -r requirements.txt
   ```
4. Docker 기반 실행 (선택)

   ```bash
   docker-compose up -d
   ```

---

### Usage

* **초기 로딩**

  * 전체 역 리스트 및 위치 기반 인근 역 조회
* **역 상세 조회**

  * 실시간 열차 도착 정보
  * 상·하행 방향별 열차 혼잡도(객차 단위)
  * 승강장 혼잡도 및 탑승 가능 여부
* **경로 조회**

  * 최단 경로 정보
  * 혼잡도 기반 경로 예측 결과
  * Redis + FastAPI 연동을 통한 시간대별 예측 데이터 제공
* **데이터 파이프라인**

  * IoT(MQTT) → Python(FastAPI) → Redis → Backend → Client
  * 실시간/과거 데이터 모두 방어 로직 포함(빈 배열, 예외 시간대 등)

---

### CI / CD

* GitLab CI/CD 파이프라인 구성
* `main` 브랜치 기준 자동 배포
* 테스트(JUnit) 및 API 검증 포함

---

### Notes

* 공공 API 한계로 일부 시간표 데이터는 CSV 기반 초기 적재
* 대용량 데이터 처리 성능 개선을 위해 캐싱 및 배치 구조 적용
* 혼잡도 알고리즘 및 수집 범위는 확장 가능하도록 설계\