# 지하철 경로 탐색 및 실시간 혼잡도 안내 Android App (Frontend)

본 프로젝트는 사용자 위치 및 입력 기반으로 지하철 **경로 검색**,  
**경로 상세 정보**, **실시간 혼잡도 및 탑승 추천**, **즐겨찾기 관리** 기능을 제공하는  
Android 애플리케이션의 프론트엔드입니다.

Jetpack Compose 기반 UI와 상태 중심 아키텍처를 사용하여  
경로 검색 → 상세 진입 → 새로고침 → 즐겨찾기 재검색까지의 흐름을  
안정적이고 일관된 UX로 제공합니다.

---

### 개발환경

- **Language**: Kotlin 2.1.x
- **UI**: Jetpack Compose (Material3)
- **Architecture**: MVVM + State Hoisting
- **Async**: Kotlin Coroutines / Flow
- **Network**: Retrofit2, OkHttpClient
- **Local DB**: Room
- **Navigation**: Navigation Compose
- **Build Tool**: Gradle (KTS)
- **Min SDK**: Android 24
- **Target SDK**: Android 36

---

### 주요 기능

#### 🚇 경로 검색
- 출발역 / 도착역 입력 기반 최단 경로 검색
- 입력 중 상태 / 입력 확정 상태 분리로 UX 안정화
- 동일 조건 검색 시 캐싱을 통한 API 중복 호출 방지
- 검색 완료 시 결과를 SharedViewModel을 통해 상세 화면으로 전달

#### 📊 경로 상세
- **경로 검색 시 전달받은 데이터(RouteResultResponse)를 기반으로 즉시 화면 구성**
- 상세 화면 진입 시 추가 API 호출 없이 빠른 렌더링 제공
- 새로고침(Pull-to-Refresh / 플로팅 버튼) 시
  - 경로 검색 엔드포인트 재호출
  - 최신 혼잡도 및 대기시간 데이터 동기화
- fetchedAt 기준 경과 시간 계산으로 남은 대기시간 실시간 감소 처리
- 로딩 / 성공 / 에러 상태를 `RouteSearchState`로 명확히 분리

#### 🧠 실시간 혼잡도 & 탑승 추천
- 열차 내 혼잡도 + 플랫폼 혼잡도 다이어그램 시각화
- 연속 3개 열차 도착 정보 표시
- 베스트 탑승 위치 / 쾌적 탑승 위치 안내
- 경로 타임라인 기반 역별 정보 구성
- 하위 컴포넌트까지 시간 정보 전달하여 화면 전체 시간 흐름 일관성 유지

#### ⭐ 즐겨찾기
- 역 / 경로 즐겨찾기 등록 및 로컬 DB(Room) 저장
- Flow 기반 즐겨찾기 목록 실시간 반영
- **즐겨찾기 경로 선택 시 저장된 결과 데이터를 사용하지 않고**
  - 출발역 / 도착역 이름 기반으로 경로 재검색
  - 항상 최신 경로 / 혼잡도 / 대기시간 정보 제공
- 즐겨찾기 목록에서는 요약 정보만 표시
  - totalTime, transferCount

#### 🎨 UX / UI
- Jetpack Compose 컴포넌트 단위 세분화 및 재사용성 강화
- 즐겨찾기 화면에서 경로 / 역 탭 스와이프 전환 지원
- 하단 시트, Snackbar, 딤 처리 등 사용자 피드백 강화
- Preview용 Fake DAO / ViewModel 구성으로 UI 테스트 용이

---

### Install

1. Android Studio 최신 버전 설치
2. 프로젝트 클론

   ```bash
   git clone <repository-url>
   ```

3. Android Studio에서 프로젝트 열기
4. Gradle Sync 후 실행

---

### Usage

1. 앱 실행 시 GPS 권한 허용
2. 위치 기반 초기 역 추천 또는 직접 출발 / 도착역 입력
3. 경로 검색 결과 확인
4. 경로 선택 → 상세 화면에서 혼잡도 및 탑승 추천 확인
5. 새로고침을 통해 최신 경로 검색 결과 동기화
6. 자주 사용하는 역 / 경로는 즐겨찾기로 관리

---

### 구조 요약

- `ui/`
  - screen, component, layout 단위 분리
- `viewmodel/`
  - 화면별 ViewModel 및 SharedViewModel
- `state/`
  - RouteSearchState, InitialLoadState 등 상태 클래스
- `domain/`
  - model, repository, util
- `data/`
  - network, response, local(Room)

---

### Notes

- 경로 상세 화면은 **검색 결과 전달 데이터 → 새로고침 시 재검색** 구조로 설계하여초기 진입 속도와 실시간 데이터 정확도를 모두 확보했습니다.
- 즐겨찾기는 **결과 캐싱이 아닌 재검색 전략**을 사용하여  
  실시간성 데이터(혼잡도, 대기시간)의 신뢰도를 보장합니다.
