# 애플리케이션 아키텍처

상태: 확정

## 구성

단일 Activity와 Navigation Compose 위에 화면별 MVP를 구성한다. Presenter는 AndroidX ViewModel로 구현하며 MVP의 Presenter 책임을 유지한다. Presenter와 Repository 사이에는 사용자 유스케이스와 비즈니스 흐름을 캡슐화하는 Application Service를 둔다.

```text
MainActivity
  -> App Navigation
      -> Compose View
             |
      Presenter (AndroidX ViewModel)
             |
      Application Service
             |
         Repository
             |
         DAO / Room

향후 Reminder/Notification 기능에서는 Application Service가 Repository와
별도 외부 시스템 계약인 AlarmScheduler를 함께 조합한다.
```

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Kotlin |
| UI | Jetpack Compose / Material 3 |
| UI 상태 | StateFlow |
| 상태 생명주기 | AndroidX ViewModel |
| 영속 저장 | Room |
| 화면 이동 | Navigation Compose |
| 비동기 | Kotlin Coroutines |
| 알림 | Android Notification API |
| 예약 | AlarmManager |
| 테스트 | JUnit, AndroidX Test, Compose UI Test |

## 상태와 이벤트

- 화면마다 불변 `UiState`와 사용자 의도를 나타내는 이벤트를 정의한다.
- Room 데이터가 영속 상태의 기준이며 Compose에 같은 데이터를 중복 저장하지 않는다.
- 일회성 메시지와 이동은 재구성·재수집 시 중복되지 않는 계약으로 설계한다.

## 계층별 책임과 의존 방향

### Compose View

- 화면을 렌더링하고 사용자 입력을 수집한다.
- 사용자 이벤트를 Presenter에 전달하고 `UiState`를 표시한다.
- Application Service, Repository, DAO와 Room에 직접 접근하지 않는다.

### Presenter

- AndroidX ViewModel을 기반으로 `UiState`와 사용자 이벤트를 관리한다.
- 화면 단위 상태를 변환하고 Application Service 결과를 UI 상태로 변환한다.
- Application Service만 호출하며 Repository, DAO와 Room에 직접 접근하지 않는다.
- Domain 비즈니스 규칙과 최종 검증을 직접 구현하지 않는다.

### Application Service

- 하나의 사용자 유스케이스를 수행하고 Domain Validator를 호출한다.
- 하나 또는 여러 Repository를 조합하고 필요한 데이터 변경 순서를 제어한다.
- Domain Model과 Repository abstraction만 사용하며 비즈니스 수준 결과를 반환한다.
- Compose, Activity, Fragment, Presenter/ViewModel, DAO, RoomDatabase와 Room Entity에 의존하지 않는다.
- DB transaction 내부 절차는 알지 않으며 Repository가 제공하는 원자적 연산을 호출한다.

### Repository

- Domain 데이터 저장·조회 abstraction을 제공한다.
- DAO 호출, Entity↔Domain 변환, DB transaction 호출 및 데이터 계층 오류 변환을 캡슐화한다.
- 상위 계층에 DAO, RoomDatabase와 Room Entity를 노출하지 않는다.

### DAO / Room

- 실제 SQL/Room 조회와 갱신, DB transaction, FK 및 schema 제약조건을 담당한다.

## 생명주기

- Presenter는 화면 범위 AndroidX ViewModel로 생성할 수 있다.
- Compose 재구성만으로 다시 생성되지 않는다.
- `viewModelScope` 등 화면 생명주기에 맞는 scope를 사용한다.
- 장기 작업은 취소와 실패를 처리한다.

## 의존성 주입

MVP에서는 Hilt/Koin 등의 DI 라이브러리를 사용하지 않는다. constructor injection을 기본으로 하며 Application 수준 `AppContainer`가 Database/DAO, Repository와 Application Service를 조립한다. 필요한 ViewModel Factory는 Repository가 아니라 Application Service를 Presenter에 주입한다. 향후 규모가 커지면 DI 라이브러리 도입을 별도로 검토한다.
