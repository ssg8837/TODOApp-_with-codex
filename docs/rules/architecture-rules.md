# 아키텍처 규칙

상태: 확정

## MVP와 AndroidX ViewModel의 관계

이 프로젝트의 논리 아키텍처는 MVP다. TRD의 `ViewModel`은 별도 MVVM 계층이 아니라 **Presenter 역할을 구현하면서 Android 화면 생명주기와 상태 보존을 담당하는 AndroidX ViewModel**을 뜻한다.

```text
Compose View
  -> 사용자 이벤트
Presenter (AndroidX ViewModel 기반 가능)
  -> Repository
  -> Room / AlarmScheduler
Presenter
  -> StateFlow<UiState>
Compose View
  -> 상태 구독 및 렌더링
```

## 책임

### View

- Activity와 Composable로 구성한다.
- `UiState` 렌더링과 사용자 이벤트 전달만 담당한다.
- Repository, Room, AlarmManager를 직접 호출하지 않는다.
- 영속 데이터의 별도 복사본을 mutable state에 보관하지 않는다.

### Presenter

- 이벤트 처리, 입력 검증, Repository 호출과 UI 상태 가공을 담당한다.
- 불변 `UiState`를 `StateFlow`로 노출한다.
- 필요하면 AndroidX `ViewModel`을 상속해 생명주기 scope를 사용한다.
- `Context`, `Activity`, Composable 또는 `NavController`를 장기 보관하지 않는다.
- 시스템 기능은 추상화된 계약을 통해 호출한다.

### Model

- Todo, Category, Reminder 및 비즈니스 규칙을 표현한다.
- Android 및 Compose 타입에 의존하지 않는다.
- ID는 `Long`, timestamp는 `Instant`를 사용한다.
- Room Entity와 Domain Model을 분리하고 mapper로 변환한다.
- Network DTO는 만들지 않으며 UI 전용 모델은 실제 화면 요구가 있을 때만 추가한다.
- `CategoryColor`는 Compose `Color`와 독립된 Domain enum으로 표현하고 UI에서 실제 색상으로 매핑한다.
- Category 순서의 Source of Truth는 Room의 `sortOrder`다.

### Repository와 Data Source

- Repository는 Room 접근과 데이터 변경의 경계를 제공한다.
- Room DAO, AlarmManager와 NotificationManager는 하위 구현 세부사항이다.
- Entity를 UI 모델로 무조건 재사용하지 않는다.
- UI 또는 Presenter 구현에 역으로 의존하지 않는다.

### 의존성 조립

- Hilt와 Koin 등의 DI 라이브러리를 MVP에 도입하지 않는다.
- constructor injection을 기본으로 한다.
- Application 수준 `AppContainer`와 필요한 ViewModel Factory로 의존성을 전달한다.
- 전역 mutable singleton을 만들지 않는다.

## 금지 사항

- Activity에 비즈니스 로직 작성
- Composable에서 영속화 또는 DAO 직접 호출
- 화면에서 Room Entity를 직접 변경
- 요구사항이 없는 계층 또는 라이브러리 추가
- `fallbackToDestructiveMigration` 계열을 기본 Room 정책으로 사용
- 요구가 없는 DTO 또는 모델 계층 추가
