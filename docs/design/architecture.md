# 애플리케이션 아키텍처

상태: 확정

## 구성

단일 Activity와 Navigation Compose 위에 화면별 MVP를 구성한다. Presenter는 AndroidX ViewModel로 구현할 수 있으며 MVP의 Presenter 책임을 유지한다.

```text
MainActivity
  -> App Navigation
      -> TodoList View <-> TodoList Presenter
      -> TodoEdit View <-> TodoEdit Presenter
      -> Category View <-> Category Presenter
                              |
                         Repository
                         /        \
                     Room      AlarmScheduler
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

## 생명주기

- Presenter는 화면 범위 AndroidX ViewModel로 생성할 수 있다.
- Compose 재구성만으로 다시 생성되지 않는다.
- `viewModelScope` 등 화면 생명주기에 맞는 scope를 사용한다.
- 장기 작업은 취소와 실패를 처리한다.

## 의존성 주입

MVP에서는 Hilt/Koin 등의 DI 라이브러리를 사용하지 않는다. constructor injection을 기본으로 하며 Application 수준 `AppContainer`가 Database, Repository와 시스템 구현을 조립한다. Presenter가 AndroidX ViewModel을 상속하는 경우 필요한 ViewModel Factory를 통해 의존성을 전달한다. 향후 규모가 커지면 DI 라이브러리 도입을 별도로 검토한다.
