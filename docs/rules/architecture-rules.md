# 아키텍처 규칙

상태: 확정

## MVP와 AndroidX ViewModel의 관계

이 프로젝트의 논리 아키텍처는 MVP다. TRD의 `ViewModel`은 별도 MVVM 계층이 아니라 **Presenter 역할을 구현하면서 Android 화면 생명주기와 상태 보존을 담당하는 AndroidX ViewModel**을 뜻한다.

```text
Compose View
  -> 사용자 이벤트
Presenter (AndroidX ViewModel 기반)
  -> Application Service
  -> Repository
  -> DAO / Room
Presenter
  -> StateFlow<UiState>
Compose View
  -> 상태 구독 및 렌더링
```

## 책임

### View

- Activity와 Composable로 구성한다.
- `UiState` 렌더링과 사용자 이벤트 전달만 담당한다.
- Application Service, Repository, DAO, Room과 AlarmManager를 직접 호출하지 않는다.
- 영속 데이터의 별도 복사본을 mutable state에 보관하지 않는다.

### Presenter

- 이벤트 처리, `UiState` 관리, 화면 단위 상태 가공과 Application Service 호출을 담당한다.
- 불변 `UiState`를 `StateFlow`로 노출한다.
- 필요하면 AndroidX `ViewModel`을 상속해 생명주기 scope를 사용한다.
- `Context`, `Activity`, Composable 또는 `NavController`를 장기 보관하지 않는다.
- Repository, DAO와 Room을 직접 호출하지 않는다.
- Domain Validator를 직접 호출하거나 비즈니스 규칙을 구현하지 않는다.
- Service 결과를 사용자용 상태와 메시지로 변환한다.

### Application Service

- 하나의 사용자 유스케이스, Domain Validator 호출과 비즈니스 규칙 실행을 담당한다.
- 하나 또는 여러 Repository와 필요 시 AlarmScheduler 같은 외부 시스템 계약을 조합한다.
- 데이터 변경의 실행 순서를 제어하되 DB transaction 내부 절차를 재구현하지 않는다.
- Domain Model과 Repository abstraction을 사용한다.
- Compose, Activity, Fragment, Presenter/ViewModel, DAO, RoomDatabase와 Room Entity에 의존하지 않는다.
- Presenter는 Service 계약에 의존하고, `Default*Service` 구현은 constructor로 Repository abstraction과 Validator를 받는다.

### Model

- Todo, Category, Reminder 및 비즈니스 규칙을 표현한다.
- Android 및 Compose 타입에 의존하지 않는다.
- ID는 `Long`, timestamp는 `Instant`를 사용한다.
- Room Entity와 Domain Model을 분리하고 mapper로 변환한다.
- Network DTO는 만들지 않으며 UI 전용 모델은 실제 화면 요구가 있을 때만 추가한다.
- `CategoryColor`는 Compose `Color`와 독립된 Domain enum으로 표현하고 UI에서 실제 색상으로 매핑한다.
- Category 순서의 Source of Truth는 Room의 `sortOrder`다.

### Repository와 Data Source

- Repository는 Domain 데이터 저장·조회 abstraction과 Room 접근 경계를 제공한다.
- Entity↔Domain 변환, DAO 호출, DB transaction 호출과 데이터 계층 오류 변환을 캡슐화한다.
- Room DAO와 RoomDatabase는 Repository의 하위 구현 세부사항이다.
- AlarmScheduler는 Repository 내부에 숨기지 않고 Application Service가 조합하는 별도 abstraction으로 유지한다.
- Entity를 UI 모델로 무조건 재사용하지 않는다.
- UI 또는 Presenter 구현에 역으로 의존하지 않는다.

### 의존성 조립

- Hilt와 Koin 등의 DI 라이브러리를 MVP에 도입하지 않는다.
- constructor injection을 기본으로 한다.
- Application 수준 `AppContainer`가 DAO, Repository와 Application Service를 조립한다.
- 필요한 ViewModel Factory는 Repository가 아니라 Application Service를 Presenter에 전달한다.
- 전역 mutable singleton을 만들지 않는다.

## 금지 사항

- Activity에 비즈니스 로직 작성
- Composable에서 영속화 또는 DAO 직접 호출
- Compose View에서 Application Service 직접 호출
- Presenter에서 Repository, DAO 또는 Room 직접 호출
- Presenter에 Domain 비즈니스 규칙 또는 최종 검증 구현
- Application Service에서 Room transaction 내부 절차 재구현
- 화면에서 Room Entity를 직접 변경
- 요구사항이 없는 계층 또는 라이브러리 추가
- `fallbackToDestructiveMigration` 계열을 기본 Room 정책으로 사용
- 요구가 없는 DTO 또는 모델 계층 추가
