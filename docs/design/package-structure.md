# 패키지 구조

상태: 확정

실제 namespace `com.example.todoapplication`을 기준으로 한다.

```text
com.example.todoapplication
├── MainActivity.kt
├── app/
│   ├── TodoApplication.kt
│   └── AppContainer.kt
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── TodoDatabase.kt
│   ├── mapper/
│   └── repository/
├── domain/
│   └── model/
├── feature/
│   ├── todo/
│   │   ├── list/
│   │   └── edit/
│   └── category/
├── navigation/
├── presenterfactory/
├── notification/
│   ├── AlarmScheduler.kt
│   ├── TodoAlarmReceiver.kt
│   └── BootReceiver.kt
└── ui/
    ├── component/
    └── theme/
```

- 각 기능 패키지에 View, Presenter, `UiState`와 이벤트를 함께 둔다.
- Repository 인터페이스의 위치는 첫 구현 시 의존 방향이 상위 계층을 향하도록 결정한다.
- 실제 공유되는 UI만 `ui/component`로 이동한다.
- 목적이 불명확한 `util` 또는 `common` 패키지를 만들지 않는다.
- MVP 규모에서는 단일 Gradle 모듈을 유지한다.
- 의존성은 constructor injection으로 전달하고 `AppContainer` 및 필요한 ViewModel Factory에서 조립한다.
