# 패키지 구조

상태: 확정

실제 namespace `com.example.todoapplication`을 기준으로 한다.

```text
com.example.todoapplication
├── MainActivity.kt
├── app/
│   ├── TodoApplication.kt
│   └── AppContainer.kt
├── application/
│   └── service/
│       ├── TodoService.kt
│       ├── DefaultTodoService.kt
│       ├── CategoryService.kt
│       ├── DefaultCategoryService.kt
│       ├── ReminderService.kt
│       └── DefaultReminderService.kt
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── TodoDatabase.kt
│   ├── mapper/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── validation/
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
- Repository abstraction은 `domain/repository`, Room 구현은 `data/repository`에 둔다.
- Application Service는 `application/service`에 두며 Domain Model, Validator와 Repository abstraction만 사용한다.
- `TodoService`, `CategoryService`, `ReminderService`는 Presenter가 의존하는 계약이며 각 `Default*Service`가 사용자 유스케이스 단위의 비즈니스 흐름을 구현한다.
- 실제 공유되는 UI만 `ui/component`로 이동한다.
- 목적이 불명확한 `util` 또는 `common` 패키지를 만들지 않는다.
- MVP 규모에서는 단일 Gradle 모듈을 유지한다.
- 의존성은 constructor injection으로 전달한다. `AppContainer`가 DAO→Repository→Application Service를 조립하고 ViewModel Factory는 Service를 Presenter에 주입한다.
