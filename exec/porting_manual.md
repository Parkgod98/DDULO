# 📑 포팅 매뉴얼 (Porting Manual) - Ddulo (뚫어)

본 문서는 **S14P11A204(뚫어)** 프로젝트의 배포를 위한 환경 설정 및 빌드 가이드를 포함합니다.

---

## 1. 빌드 및 배포 가이드

### 1.1 개발 및 실행 환경
* **OS**: Windows 10/11 (Development), Linux (Deployment)
* **IDE**: IntelliJ IDEA 2023.3, 2025.3
* **Java**: OpenJDK 21
* **Framework**: Spring Boot 3.5.9
* **Build Tool**: Maven 3.x
* **Database**: MySQL 8.0, Redis
* **WAS**: Embedded Tomcat (Spring Boot 내장)
* **API Documentation**: OpenAPI 3.0 (SpringDoc 2.8.13)

### 1.2 주요 환경 변수 및 비밀키 설정
배포 시 `application-secret.properties` 혹은 시스템 환경 변수에 아래 항목들이 반드시 정의되어 있어야 합니다.

| 구분 | 환경 변수명 | 비고 |
| :--- | :--- | :--- |
| **SK API** | `SK_API_APPKEY` | 실시간 혼잡도 데이터용 (l7xx...) |
| **서울 데이터** | `SEOUL_API_APPKEY` | 빠른 하차 및 최단 경로용 (6550...) |
| **지하철 실시간** | `SEOUL_API_SUBWAY_APPKEY` | 실시간 열차 위치 정보용 (535a...) |
| **공공 데이터** | `DATA_API_ENCODING_APPKEY` | 철도역 빠른 환승 정보용 |
| **DB 계정** | `MYSQL_USERNAME` / `MYSQL_PASSWORD` | MySQL 접속 정보 (기본: ddulo / 1234d) |
| **Redis** | `REDIS_PWD` | Redis 접속 비밀번호 (기본: 1234d) |

### 1.3 빌드 및 실행 절차

1. **인프라 구성 (Docker)**
   ```bash
   # MySQL 및 Redis 컨테이너 실행
   docker-compose up -d
   ```

2. **애플리케이션 빌드**
   ```bash
   ./mvnw clean package -DskipTests
   ```   

3. **애플리케이션 실행**
   ```bash
   java -jar target/ddulo-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```   

---

## 2. 외부 서비스 정보

| 외부 서비스 | 가입 및 활용 정보 | 비고 |
| :--- | :--- | :--- |
| **SK Open API** | SK API Portal 가입 후 `AppKey` 발급 필요 | 전철역 메타데이터 확보 |
| **서울 열린데이터광장** | 공공데이터 신청 후 `인증키` 발급 | 실시간 위치/경로, 하차정보, 열차 시간표 |
| **공공데이터포털** | 국토교통부 API 활용 신청 | 환승 정보 |
| **자체 Python API** | `:8001` 포트로 구동되는 분석 서버 연동 | 알고리즘 연동 |

---

## 3. 데이터베이스 설정
* **Entity 기반 스키마 생성**: JPA Entity 클래스 설정을 기반으로 애플리케이션 기동 시 테이블이 자동 생성됩니다.
* **초기 데이터 로드**: 역 정보 등 필수 마스터 데이터는 `OpenCSV` 라이브러리를 통해 초기 기동 시점에 로드하거나 데이터베이스에 직접 삽입합니다.

---

## 4. 시연 시나리오

1. **서비스 접속**: 모바일 앱 실행 후 메인 화면 노출 확인.
2. **역 정보 확인**: 검색바에서 역을 조회 후, 원하는 역을 눌러 **역 상세 정보(양 방향 가까운 열차 3대의 정보)**를 확인합니다.
3. **경로 검색**: **출발역**과 **도착역**을 입력하여 검색합니다.
4. **추천 결과 확인**: 검색된 경로의 **예상 탑승 시각**과 **탑승 확률** 정보를 확인합니다.
5. **최종 결과 확인**: 검색된 경로를 탭하여 **객차 및 플랫폼 혼잡도**와 **베스트 & 쾌적 탑승** 및 **시간 정보**등을 확인합니다.