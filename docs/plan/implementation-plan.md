# TODO App MVP 구현 계획

상태: 구현 전 확정 계획  
기준: `AGENTS.md`, `docs/requirements/`, `docs/design/`, `docs/rules/`

## 공통 진행 원칙

- Phase는 순서대로 누적하며 이전 Phase의 테스트를 계속 통과시킨다.
- 각 Phase 종료 시 최소한 `./gradlew assembleDebug`가 성공해야 한다.
- JVM에서 검증 가능한 로직은 `./gradlew testDebugUnitTest`로 검증한다.
- Room 및 Compose 계측 테스트는 에뮬레이터/기기에서 `./gradlew connectedDebugAndroidTest`로 검증한다.
- 계측 환경이 없으면 미실행 범위를 기록하고 다음 Phase로 넘기기 전에 빌드와 JVM 테스트를 통과시킨다.
- Room을 영속 데이터의 Source of Truth로 사용한다.
- 논리 구조는 MVP이며 AndroidX ViewModel은 Presenter의 생명주기 기반으로 사용한다.
- 의존 방향은 Compose View→Presenter→Application Service→Repository→DAO/Room으로 유지한다.
- Hilt/Koin 없이 constructor injection을 사용하며 `AppContainer`가 Repository와 Application Service를 조립하고 ViewModel Factory는 Service를 주입한다.
- 사용자 노출 문자열은 리소스로 관리한다.
- 요구사항에 없는 기능, Network DTO 및 불필요한 모델 계층을 추가하지 않는다.
- 각 Phase 완료 보고에는 변경 파일, 충족 요구사항, 실행한 테스트, 결과와 남은 제한을 기록한다.

## 현재 프로젝트 기준선

- 단일 Gradle 모듈: `:app`
- namespace/applicationId: `com.example.todoapplication`
- minSdk 24, targetSdk/compileSdk 37
- Kotlin 2.2.10, AGP 9.3.2, Gradle Wrapper 9.5.0
- Jetpack Compose와 Material 3만 적용된 기본 `Greeting` 화면
- Room, Navigation Compose, ViewModel Compose, Coroutines, KSP 의존성 미도입
- 템플릿 수준의 JVM 테스트와 계측 테스트만 존재

## Phase 0: 현재 프로젝트 및 빌드 기준선 확인

### 목적

코드 변경 전 현재 프로젝트가 재현 가능하게 빌드되는지 확인하고 이후 Phase의 비교 기준을 남긴다.

### 구현 대상

- 현재 Gradle, Manifest, Kotlin/Compose 소스와 테스트 구조의 읽기 전용 점검
- 사용 가능한 JDK, SDK, 에뮬레이터와 Gradle task 확인

### 생성 또는 수정할 예상 파일

- 없음
- 실행 결과를 별도 기록해야 한다면 사용자 승인 범위에서 작업 보고에만 기록

### 의존하는 이전 Phase

- 없음

### 구현 상세

- Gradle Wrapper와 Java 실행 환경을 확인한다.
- `assembleDebug`, JVM 테스트와 계측 테스트의 현재 성공 여부를 기록한다.
- 기존 `Greeting` 템플릿, package와 Manifest 시작 Activity를 확인한다.
- 기존 경고와 이후 변경으로 생긴 회귀를 구분할 기준선을 만든다.

### 테스트 항목

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest`
- 에뮬레이터가 있으면 `./gradlew connectedDebugAndroidTest`

### 완료 조건

- 현재 빌드 및 테스트 결과와 실행 환경이 기록되어 있다.
- 실패가 있다면 원인과 다음 Phase 진행 가능 여부가 구분되어 있다.
- 앱 파일이 변경되지 않았다.

### 해당 Phase에서 수정하면 안 되는 범위

- 모든 Kotlin, Gradle, Manifest 및 리소스 파일
- 의존성 버전과 SDK 설정
- 기존 테스트 코드

## Phase 1: Domain Model / Room Entity / Mapper

### 목적

확정된 Domain 모델과 Room 저장 모델을 분리하고 안정적인 변환 경계를 만든다.

### 구현 대상

- Room/KSP/Coroutines/ViewModel/Navigation 및 Java time desugaring의 최소 빌드 기반
- `Todo`, `Category`, `Reminder`, `CategoryColor` Domain 모델
- 각 Room Entity와 날짜·시간·색상 converter
- Entity↔Domain mapper와 입력 검증

### 생성 또는 수정할 예상 파일

- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/java/com/example/todoapplication/domain/model/Todo.kt`
- `app/src/main/java/com/example/todoapplication/domain/model/Category.kt`
- `app/src/main/java/com/example/todoapplication/domain/model/Reminder.kt`
- `app/src/main/java/com/example/todoapplication/domain/model/CategoryColor.kt`
- `app/src/main/java/com/example/todoapplication/domain/validation/TodoValidator.kt`
- `app/src/main/java/com/example/todoapplication/domain/validation/CategoryValidator.kt`
- `app/src/main/java/com/example/todoapplication/data/local/entity/TodoEntity.kt`
- `app/src/main/java/com/example/todoapplication/data/local/entity/CategoryEntity.kt`
- `app/src/main/java/com/example/todoapplication/data/local/entity/ReminderEntity.kt`
- `app/src/main/java/com/example/todoapplication/data/local/RoomConverters.kt`
- `app/src/main/java/com/example/todoapplication/data/mapper/TodoMapper.kt`
- `app/src/main/java/com/example/todoapplication/data/mapper/CategoryMapper.kt`
- `app/src/main/java/com/example/todoapplication/data/mapper/ReminderMapper.kt`
- `app/src/test/java/com/example/todoapplication/domain/validation/*Test.kt`
- `app/src/test/java/com/example/todoapplication/data/mapper/*Test.kt`

### 의존하는 이전 Phase

- Phase 0

### 구현 상세

- Domain ID는 `Long`, timestamp는 `Instant`로 표현한다.
- DB PK는 auto-generated `Long`, timestamp는 epoch milliseconds `Long`으로 표현한다.
- `Todo.categoryId`는 NOT NULL이다.
- Category에 `color`, `sortOrder`, `isSystem`, `createdAt`을 포함한다.
- `CategoryColor`는 8개의 의미 enum으로 만들고 Compose `Color`에 의존하지 않는다.
- `LocalDate`, `LocalTime?`, `Instant`, `CategoryColor`를 안정적인 DB 값으로 변환한다.
- mapper와 validator를 Android UI 없이 테스트 가능하게 만든다.

### 테스트 항목

- Domain↔Entity 왕복 변환
- 날짜 `epochDay`, 시간 `minuteOfDay`, timestamp epoch milliseconds 왕복
- CategoryColor 문자열 저장값 왕복
- 공백 제목·Category 이름, 시간 없는 Reminder 검증
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

### 완료 조건

- 모든 모델과 mapper가 컴파일된다.
- Entity와 Domain Model이 분리되어 있다.
- Domain 계층이 Room, Android, Compose 타입에 의존하지 않는다.
- 변환 및 검증 단위 테스트가 통과한다.

### 해당 Phase에서 수정하면 안 되는 범위

- Activity 및 Composable 화면 동작
- Navigation
- DAO, Database와 Repository 구현
- Notification, AlarmManager와 권한

## Phase 2: DAO / Room Database

### 목적

로컬 영속화와 transaction 규칙을 UI 없이 검증 가능한 상태로 완성한다.

### 구현 대상

- Todo, Category, Reminder DAO
- Room Database version 1
- 최초 생성 시 시스템 Category `일반` seed
- Category 삭제 및 재정렬 transaction
- Todo 기본 정렬과 관계 삭제

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/data/local/dao/TodoDao.kt`
- `app/src/main/java/com/example/todoapplication/data/local/dao/CategoryDao.kt`
- `app/src/main/java/com/example/todoapplication/data/local/dao/ReminderDao.kt`
- `app/src/main/java/com/example/todoapplication/data/local/TodoDatabase.kt`
- `app/src/main/java/com/example/todoapplication/data/local/DatabaseInitializer.kt`
- `app/src/main/java/com/example/todoapplication/data/local/CategoryTransactions.kt`
- `app/src/androidTest/java/com/example/todoapplication/data/local/TodoDaoTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/data/local/CategoryDaoTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/data/local/ReminderDaoTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/data/local/TodoDatabaseTest.kt`

### 의존하는 이전 Phase

- Phase 1

### 구현 상세

- DB version을 `1`로 선언하고 destructive migration fallback을 사용하지 않는다.
- 최초 DB 생성 시 `일반/NEUTRAL/sortOrder 0/isSystem true`를 삽입한다.
- Category 이름 uniqueness와 Todo의 NOT NULL FK를 DB에서도 보장한다.
- Todo 삭제 시 Reminder를 cascade 삭제한다.
- 사용자 Category 삭제는 연결 Todo를 `일반`로 갱신한 후 Category를 삭제하는 하나의 transaction으로 처리한다.
- 사용자 Category 재정렬은 `sortOrder 1..N`을 하나의 transaction에서 갱신한다.
- Todo 조회는 유시간 우선, 시간 오름차순, 무시간, 동시간 createdAt 오름차순을 보장한다.
- DAO 관찰 결과는 필요한 곳에서 `Flow`로 제공한다.

### 테스트 항목

- 각 Entity CRUD와 앱 DB 재개방 후 유지
- 최초 DB 생성 시 `일반` 존재 및 수정·삭제 보호
- 중복 Category 이름 거부
- Todo 삭제 시 Reminder 삭제
- 사용 중인 Category 삭제 시 Todo 보존·`일반` 재지정·원자성
- 신규 Category 마지막 순서와 재정렬 연속성
- Todo 기본 정렬 및 완료 여부가 정렬에 미치는 영향 없음
- `./gradlew connectedDebugAndroidTest`
- `./gradlew assembleDebug`

### 완료 조건

- in-memory 및 실제 파일 Room DB 테스트가 통과한다.
- 모든 확정된 관계, constraint, transaction과 정렬이 DB에서 보장된다.
- schema export 등 Room 빌드 산출 구성이 정상이다.

### 해당 Phase에서 수정하면 안 되는 범위

- Presenter와 UiState
- Compose UI와 Navigation
- Repository 외부 API
- Notification과 Alarm

## Phase 3: Repository

### 목적

Application Service가 DAO/Entity를 알지 않고 Domain 모델만 사용하도록 데이터 접근 경계를 만든다.

### 구현 대상

- Todo, Category, Reminder Repository 계약과 Room 구현
- DB transaction의 Domain API
- 데이터 계층 오류 변환

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/domain/repository/TodoRepository.kt`
- `app/src/main/java/com/example/todoapplication/domain/repository/CategoryRepository.kt`
- `app/src/main/java/com/example/todoapplication/domain/repository/ReminderRepository.kt`
- `app/src/main/java/com/example/todoapplication/data/repository/RoomTodoRepository.kt`
- `app/src/main/java/com/example/todoapplication/data/repository/RoomCategoryRepository.kt`
- `app/src/main/java/com/example/todoapplication/data/repository/RoomReminderRepository.kt`
- `app/src/test/java/com/example/todoapplication/data/repository/*Test.kt`

### 의존하는 이전 Phase

- Phase 2

### 구현 상세

- Entity 변환과 DAO 호출을 Repository 내부에 캡슐화한다.
- Todo 관찰·등록·수정·삭제·완료 변경 계약을 제공한다.
- Category 관찰·생성·수정·삭제·재정렬 계약을 제공한다.
- Reminder 저장·조회·삭제와 미래 Reminder 조회 계약을 제공한다.
- Room/SQLite 실패를 상위 계층이 해석할 수 있는 데이터 계층 오류로 변환한다.
- Hilt/Koin 없이 constructor injection을 사용한다.
- Service 유스케이스, Validator 호출, 여러 Repository 조합과 비즈니스 작업 순서는 구현하지 않는다.

### 테스트 항목

- DAO 결과의 Domain 변환
- 저장, 조회, 수정, 삭제와 실패 전달
- Category 삭제·재정렬 transaction 호출
- Flow 관찰 및 구독 갱신
- Room/SQLite 오류의 데이터 계층 오류 변환
- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest`

### 완료 조건

- 상위 계층에 DAO와 Entity가 노출되지 않는다.
- Repository 테스트가 성공·실패 및 Flow 갱신을 검증한다.
- Repository abstraction이 Room 구현과 분리되고 데이터 계층 오류 계약이 정의되어 있다.

### 해당 Phase에서 수정하면 안 되는 범위

- 화면별 Presenter와 UiState
- Application Service, Validator 호출과 유스케이스 조합
- `AppContainer` 및 ViewModel Factory 조립
- Compose 화면 및 Navigation
- 시스템 알람 및 알림 권한
- 요구되지 않은 remote/network 계층

## Phase 4: Application Service

### 목적

UI와 저장소 사이의 Todo, Category와 Reminder 사용자 유스케이스 및 비즈니스 흐름을 Android UI 없이 실행 가능한 경계로 만든다.

### 구현 대상

- `TodoService`, `CategoryService`, `ReminderService`
- 기존 Domain Validator 연계
- 하나 또는 여러 Repository 조합과 비즈니스 수준 결과
- Application 수준 수동 의존성 조립
- Fake Repository 기반 Service 단위 테스트

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/application/service/TodoService.kt`
- `app/src/main/java/com/example/todoapplication/application/service/DefaultTodoService.kt`
- `app/src/main/java/com/example/todoapplication/application/service/CategoryService.kt`
- `app/src/main/java/com/example/todoapplication/application/service/DefaultCategoryService.kt`
- `app/src/main/java/com/example/todoapplication/application/service/ReminderService.kt`
- `app/src/main/java/com/example/todoapplication/application/service/DefaultReminderService.kt`
- `app/src/main/java/com/example/todoapplication/application/service/ServiceResult.kt`
- `app/src/main/java/com/example/todoapplication/app/AppContainer.kt`
- `app/src/main/java/com/example/todoapplication/app/DefaultAppContainer.kt`
- `app/src/test/java/com/example/todoapplication/application/service/*Test.kt`
- `app/src/test/java/com/example/todoapplication/test/Fake*Repository.kt`

### 의존하는 이전 Phase

- Phase 3

### 구현 상세

- Presenter는 `TodoService`, `CategoryService`, `ReminderService` 계약에 의존하고 각 `Default*Service`가 유스케이스를 구현한다.
- `DefaultTodoService`는 Todo 등록·수정·삭제·완료 변경·날짜별 조회와 `TodoValidator` 적용을 담당한다.
- `DefaultCategoryService`는 생성·수정·삭제·순서 변경, 시스템 Category 보호와 `CategoryValidator` 적용을 담당한다.
- `DefaultReminderService`는 Reminder 등록·교체·삭제와 `ReminderValidator` 적용을 담당한다. 교체는 검증 후 Repository의 단일 원자적 교체 API를 호출한다.
- 유효성 검증에 실패하면 Repository 변경을 호출하지 않는다.
- 여러 Repository가 필요한 유스케이스의 호출 순서와 부분 실패 정책을 Service에 캡슐화한다.
- Category 삭제·재정렬과 Reminder 교체의 DB transaction 내부 절차는 다시 구현하지 않고 Repository의 원자적 API를 호출한다.
- Service는 Domain Model과 Repository abstraction만 사용하며 Compose, Android UI, DAO, RoomDatabase와 Room Entity에 의존하지 않는다.
- `AppContainer`가 Database/DAO→Repository→Service를 constructor injection으로 조립한다.

### 테스트 항목

- 세 Validator의 적용과 유효성 실패 시 Repository 미호출
- Todo, Category와 Reminder 성공·실패 유스케이스
- 시스템 Category 수정·삭제 방지와 순서 정책
- 여러 Repository 호출 순서 및 오류 시 후속 호출 중단 여부
- Category transaction API 위임 및 Service에서 내부 절차를 재구현하지 않음
- Reminder 교체 Repository API 단일 호출, 실패 rollback과 부분 반영 방지
- Fake Repository 기반 `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

### 완료 조건

- Presenter가 사용할 세 Service 계약과 구현이 준비되어 있다.
- 비즈니스 규칙과 Validator가 Service 단위 테스트로 검증된다.
- 상위 계층에 DAO, RoomDatabase, Room Entity와 내부 SQLite 예외가 노출되지 않는다.
- `AppContainer`가 Repository와 Service를 수동 조립한다.

### 해당 Phase에서 수정하면 안 되는 범위

- Presenter, UiState와 ViewModel Factory
- Compose 화면 및 Navigation
- DAO transaction 내부 구현 변경
- AlarmScheduler, Notification과 Android 권한 구현

## Phase 5: TODO 목록 Presenter 및 UiState

### 목적

오늘/날짜 이동, 정렬 목록과 완료 상태 변경을 UI 없이 검증한다.

### 구현 대상

- TodoList UiState, Event와 Presenter
- 날짜/시간을 테스트하기 위한 Clock 경계
- ViewModel Factory와 Presenter 테스트 도구

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListEvent.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListPresenter.kt`
- `app/src/main/java/com/example/todoapplication/presenterfactory/TodoListPresenterFactory.kt`
- `app/src/main/java/com/example/todoapplication/domain/time/ClockProvider.kt`
- `app/src/test/java/com/example/todoapplication/feature/todo/list/TodoListPresenterTest.kt`
- `app/src/test/java/com/example/todoapplication/test/MainDispatcherRule.kt`
- `app/src/test/java/com/example/todoapplication/test/FakeClockProvider.kt`

### 의존하는 이전 Phase

- Phase 4

### 구현 상세

- 초기 `selectedDate`를 Clock 기준 오늘로 설정한다.
- 이전/다음 날짜 및 DatePicker 선택 날짜 이벤트를 처리한다.
- `TodoService`의 정렬된 Todo Flow를 불변 `StateFlow<UiState>`로 노출한다.
- 완료/미완료 변경을 처리하고 저장 실패 시 UI와 DB 상태를 일치시킨다.
- 날짜 빈 상태, 로딩과 조회 오류를 구분한다.
- 이 Phase에서는 Category/미완료 필터를 적용하지 않는다.

### 테스트 항목

- 고정 Clock 기준 오늘 초기화
- 이전/다음 및 임의 날짜 선택
- 정렬 목록 반영과 빈 상태
- 완료/미완료 성공과 저장 실패 복구
- 조회 실패의 사용자 메시지 변환
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

### 완료 조건

- Presenter가 Activity, Composable, NavController, Repository, DAO와 Room에 의존하지 않고 Service만 사용한다.
- 날짜와 완료 상태의 주요 상태 전이 테스트가 통과한다.
- 앱은 기존 화면 상태로 계속 빌드된다.

### 해당 Phase에서 수정하면 안 되는 범위

- Compose 제품 화면과 Navigation
- Repository 직접 호출 및 Presenter 내부 비즈니스 검증
- Todo 등록/수정
- Category 관리와 필터
- Reminder, Notification과 Alarm

## Phase 6: TODO 목록 Compose UI

### 목적

Todo 목록 Presenter 상태를 렌더링하는 첫 제품 화면과 최소 Navigation 셸을 만든다.

### 구현 대상

- TodoList Route/Screen/Item
- 날짜 이동 버튼과 DatePicker
- 빈 상태와 오류 표시
- 단일 Activity 앱 셸과 Navigation Compose 시작 목적지

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/MainActivity.kt`
- `app/src/main/java/com/example/todoapplication/app/TodoApplication.kt`
- `app/src/main/java/com/example/todoapplication/app/TodoApp.kt`
- `app/src/main/java/com/example/todoapplication/navigation/Destination.kt`
- `app/src/main/java/com/example/todoapplication/navigation/TodoNavHost.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListRoute.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListScreen.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoItem.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/EmptyState.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/ErrorMessage.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/example/todoapplication/feature/todo/list/TodoListScreenTest.kt`

### 의존하는 이전 Phase

- Phase 5

### 구현 상세

- `Greeting`을 앱 셸과 TodoList 시작 화면으로 교체한다.
- 생명주기를 고려해 Presenter `StateFlow`를 수집한다.
- 이전/다음 날짜 버튼과 현재 날짜 선택 DatePicker를 제공한다.
- Todo 시간, 제목, Category 이름/색상과 완료 상태를 표시한다.
- Category 색상만으로 의미를 전달하지 않고 이름을 함께 표시한다.
- 추가/수정/Category 목적지는 route만 준비하고 아직 기능을 연결하지 않는다.

### 테스트 항목

- 오늘 날짜, 이전/다음 날짜와 DatePicker 이벤트
- Todo 렌더링과 완료 토글
- 날짜 빈 상태, 로딩과 오류 상태
- 색상과 Category 이름 동시 표시 및 접근성 semantics
- 시작 목적지와 기본 Navigation 셸
- Preview 및 `./gradlew connectedDebugAndroidTest`

### 완료 조건

- 앱 실행 시 TodoList 화면이 표시된다.
- 날짜 조회와 완료 변경이 UI에서 동작한다.
- Compose UI 테스트와 `assembleDebug`가 통과한다.

### 해당 Phase에서 수정하면 안 되는 범위

- Todo 등록/수정/삭제 구현
- Category 관리 및 drag & drop
- Category/미완료 필터
- Reminder와 시스템 알람

## Phase 7: TODO 등록 / 수정

### 목적

Reminder를 제외한 Todo 등록·수정·삭제 흐름을 완성하고 목록과 영속화를 연결한다.

### 구현 대상

- TodoEdit Presenter, UiState, Event와 Factory
- TodoEdit Route/Screen
- 제목, 날짜, 선택 시간, Category 입력
- 저장, ID 기반 수정 조회와 AlertDialog 삭제
- TodoList↔TodoEdit Navigation

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditEvent.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditPresenter.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditRoute.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditScreen.kt`
- `app/src/main/java/com/example/todoapplication/presenterfactory/TodoEditPresenterFactory.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/TodoDateField.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/TodoTimeField.kt`
- `app/src/main/java/com/example/todoapplication/navigation/TodoNavHost.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/example/todoapplication/feature/todo/edit/TodoEditPresenterTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/feature/todo/edit/TodoEditScreenTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/TodoCrudFlowTest.kt`

### 의존하는 이전 Phase

- Phase 6

### 구현 상세

- 신규 화면은 진입 날짜를 기본 날짜로 사용한다.
- Category 미선택 시 시스템 `일반` ID를 적용하고 Category 없는 Todo 저장을 막는다.
- 과거 날짜와 시간을 허용한다.
- 수정 화면에는 Todo ID만 전달하고 `TodoService`에서 다시 조회한다.
- 저장 직전 최종 검증은 Service에 위임하고 Presenter는 중복 저장 방지와 실패 시 편집값 유지를 구현한다.
- TODO 삭제는 AlertDialog 확인 후 수행하고 Todo/Reminder 비즈니스 흐름은 Service에 위임한다.
- 성공 이동과 메시지는 재구성 시 중복되지 않게 처리한다.

### 테스트 항목

- 제목/날짜 검증과 기본 `일반` 적용
- 과거 날짜·시간 저장
- 신규 등록, ID 조회, 수정과 존재하지 않는 ID
- 저장 중 중복 요청 및 실패 시 입력 유지
- AlertDialog 취소/확정과 삭제 실패
- 저장 후 목록 반영 및 앱 재실행 후 유지
- Presenter JVM 테스트, CRUD Compose/Room 계측 테스트

### 완료 조건

- Reminder를 제외한 Todo 등록·조회·수정·삭제·완료 변경이 동작한다.
- UI와 DB 상태가 일치하고 재실행 후 데이터가 유지된다.
- CRUD 관련 자동 테스트와 빌드가 통과한다.

### 해당 Phase에서 수정하면 안 되는 범위

- Category 생성·수정·삭제 UI
- Category drag & drop과 팔레트
- 복합 필터
- Reminder 선택, Notification과 Alarm

## Phase 8: Category 관리

### 목적

시스템 Category 보호와 사용자 Category CRUD/삭제 transaction을 Presenter 및 기본 화면에서 완성한다.

### 구현 대상

- Category Presenter, UiState, Event와 Factory
- CategoryManagement Route/Screen
- 이름 검증, 생성·수정 및 AlertDialog 삭제
- `일반` 보호와 사용 중 Category 재지정 안내

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/feature/category/CategoryUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryEvent.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryPresenter.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryRoute.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryScreen.kt`
- `app/src/main/java/com/example/todoapplication/presenterfactory/CategoryPresenterFactory.kt`
- `app/src/main/java/com/example/todoapplication/navigation/TodoNavHost.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/example/todoapplication/feature/category/CategoryPresenterTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/feature/category/CategoryScreenTest.kt`

### 의존하는 이전 Phase

- Phase 7

### 구현 상세

- `일반`과 사용자 Category를 저장 순서로 관찰한다.
- 공백 및 전체 Category 이름 중복의 최종 검증은 `CategoryService`가 수행한다.
- 신규 Category의 마지막 `sortOrder` 결정은 `CategoryService`가 Repository 조회와 저장을 조합한다.
- `일반`의 이름 수정·삭제는 `CategoryService`에서 차단하고 Repository/Room 제약도 방어선으로 유지한다.
- 사용자 Category 삭제 AlertDialog에 연결 Todo가 삭제되지 않고 `일반`로 변경됨을 안내한다.
- Category 색상 값은 상태와 저장 계약에 포함하되 팔레트 UI 및 재정렬 UI는 Phase 9에서 완성한다.

### 테스트 항목

- `일반` 항상 존재 및 수정·삭제 차단
- 공백/중복/`일반` 이름 생성 차단
- 사용자 Category 생성·이름 수정·삭제
- 사용 중 Category 삭제 시 Todo 보존 및 `일반` 재지정
- 실패 시 기존 목록과 입력 유지
- 화면 Navigation과 AlertDialog

### 완료 조건

- Category 핵심 CRUD와 보호 정책이 자동 테스트된다.
- Category 삭제 transaction 결과가 목록과 Todo에 반영된다.
- 앱 빌드 및 기존 Todo CRUD 회귀 테스트가 통과한다.

### 해당 Phase에서 수정하면 안 되는 범위

- drag & drop 구현
- 색상 팔레트 UI와 실제 Compose 색상 토큰
- TODO 필터
- Reminder와 시스템 알람

## Phase 9: Category 정렬 및 색상

### 목적

Category의 사용자 지정 순서와 사전 정의 색상 UI를 완성하고 모든 Category 목록에 동일하게 적용한다.

### 구현 대상

- 사용자 Category drag & drop 및 순서 저장
- Category 색상 팔레트와 Domain→Compose 색상 매핑
- 생성/수정 화면 색상 선택
- TODO 편집 Category 선택 목록 순서·색상 반영

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/feature/category/CategoryPresenter.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryScreen.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/CategoryColorPicker.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/CategoryRow.kt`
- `app/src/main/java/com/example/todoapplication/ui/theme/CategoryColors.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditScreen.kt`
- `app/src/test/java/com/example/todoapplication/feature/category/CategoryReorderTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/feature/category/CategoryReorderUiTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/feature/category/CategoryColorPickerTest.kt`

### 의존하는 이전 Phase

- Phase 8

### 구현 상세

- `일반`은 `sortOrder 0`으로 최상단에 고정하고 drag handle을 표시하지 않는다.
- 사용자 Category끼리만 이동시키고 `CategoryService`가 결과를 `1..N`으로 재계산해 Repository의 원자적 순서 저장 API를 호출한다.
- 작은 원형 버튼에서 8개 사전 정의 팔레트를 열어 색상을 선택한다.
- 자유 RGB/HEX 입력은 제공하지 않고 같은 색상 중복 사용을 허용한다.
- `NEUTRAL` 등 Domain enum을 UI의 구체적인 Material 색상으로 매핑한다.
- 색상과 Category 이름을 함께 표시하고 접근성 설명을 제공한다.
- 앱 재실행 후와 Todo 등록/수정 Category 목록에서 저장 순서를 재사용한다.

### 테스트 항목

- 신규 Category 마지막 추가
- `일반` 이동 불가 및 사용자끼리 drag & drop
- 이동 후 `sortOrder 1..N`, transaction 실패 시 원상 유지
- 앱 재실행 후 순서 유지
- 팔레트 선택, 동일 색상 허용, `일반` 색상 변경 차단
- 색상과 이름 동시 표시 및 접근성 semantics

### 완료 조건

- Category 순서와 색상 요구사항이 관리 및 Todo 편집 화면에서 동작한다.
- DB, Presenter와 Compose UI 테스트가 통과한다.
- 기존 Category CRUD와 Todo CRUD 테스트가 계속 통과한다.

### 해당 Phase에서 수정하면 안 되는 범위

- TODO 목록 Category 필터 동작
- TODO 기본 시간 정렬
- Reminder와 Notification
- 자유 색상 입력 또는 팔레트 외 색상

## Phase 10: TODO 필터

### 목적

날짜, Category와 미완료 조건의 AND 복합 필터를 Presenter와 UI에 추가한다.

### 구현 대상

- 필터 DAO/Repository 계약
- TodoService 필터 조회 계약
- TodoList UiState/Event/Presenter 필터 상태
- Category 필터 및 미완료 UI
- 필터 빈 상태

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/data/local/dao/TodoDao.kt`
- `app/src/main/java/com/example/todoapplication/domain/repository/TodoRepository.kt`
- `app/src/main/java/com/example/todoapplication/data/repository/RoomTodoRepository.kt`
- `app/src/main/java/com/example/todoapplication/application/service/TodoService.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListEvent.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListPresenter.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListScreen.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/CategoryFilter.kt`
- `app/src/test/java/com/example/todoapplication/feature/todo/list/TodoFilterPresenterTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/data/local/TodoFilterDaoTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/feature/todo/list/TodoFilterUiTest.kt`

### 의존하는 이전 Phase

- Phase 9

### 구현 상세

- 날짜는 항상 필수 조건으로 사용한다.
- Category는 `전체` 또는 기존 Category ID를 사용한다.
- `incompleteOnly`를 Category 조건과 AND로 결합한다.
- Category 필터 목록은 `일반` 최상단 및 저장된 `sortOrder`를 사용한다.
- 필터를 바꿔도 문서화된 Todo 시간/생성순 정렬을 유지한다.
- 날짜 자체 빈 상태와 필터 결과 없음 상태를 구분한다.
- 필터 선택값은 앱 재실행 간 영속화하지 않는다.

### 테스트 항목

- 날짜 단독, Category 단독, 미완료 단독 및 세 조건 AND 조합
- `전체` 선택 시 Category 조건 해제
- 존재하지 않는 Category 필터 안전 처리
- 필터 결과 정렬 유지
- Category 순서와 색상/이름 표시
- 완료 처리 시 미완료 결과에서 즉시 제외
- DAO, Presenter 및 Compose UI 필터 테스트

### 완료 조건

- 모든 필터 조합과 빈 상태가 자동 테스트된다.
- CRUD 후 필터 결과가 Room Flow를 통해 즉시 갱신된다.
- Phase 0~9 회귀 테스트와 빌드가 통과한다.

### 해당 Phase에서 수정하면 안 되는 범위

- 필터 상태 영속화
- 검색, 반복 일정 또는 새로운 정렬 옵션
- Reminder 선택 및 시스템 알람
- Category 관리 정책 변경

## Phase 11: Reminder / Notification / AlarmManager

### 목적

안정화된 CRUD/필터 위에 Reminder 저장, 권한 처리, Alarm 예약과 Notification 표시를 단계적으로 연결한다.

### 구현 대상

- Reminder 시각 계산과 TodoEdit 입력
- TodoService/ReminderService의 Repository와 AlarmScheduler 조합
- AlarmScheduler 계약 및 Android AlarmManager 구현
- Exact→Inexact fallback
- Notification Channel/Factory/Receiver
- 알림 권한 요청과 예약 실패 안내
- Todo 수정·삭제·완료 상태에 따른 Alarm 갱신

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/domain/reminder/ReminderCalculator.kt`
- `app/src/main/java/com/example/todoapplication/application/service/TodoService.kt`
- `app/src/main/java/com/example/todoapplication/application/service/ReminderService.kt`
- `app/src/main/java/com/example/todoapplication/notification/AlarmScheduler.kt`
- `app/src/main/java/com/example/todoapplication/notification/AndroidAlarmScheduler.kt`
- `app/src/main/java/com/example/todoapplication/notification/AlarmIdentity.kt`
- `app/src/main/java/com/example/todoapplication/notification/NotificationChannels.kt`
- `app/src/main/java/com/example/todoapplication/notification/NotificationFactory.kt`
- `app/src/main/java/com/example/todoapplication/notification/TodoAlarmReceiver.kt`
- `app/src/main/java/com/example/todoapplication/notification/NotificationPermissionController.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEdit*.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListPresenter.kt`
- `app/src/main/java/com/example/todoapplication/app/DefaultAppContainer.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/example/todoapplication/domain/reminder/ReminderCalculatorTest.kt`
- `app/src/test/java/com/example/todoapplication/notification/*Test.kt`
- `app/src/androidTest/java/com/example/todoapplication/notification/AlarmSchedulerTest.kt`

### 의존하는 이전 Phase

- Phase 10 및 CRUD/필터 전체 회귀 테스트 성공

### 구현 상세

- 체크포인트 11A: `15`, `1440` Reminder 선택·계산·Room 저장까지만 구현하고 빌드/테스트한다.
- 체크포인트 11B: Notification Channel, Receiver와 Notification 생성을 구현하고 빌드/테스트한다.
- 체크포인트 11C: 안정적인 Todo+Reminder Alarm ID와 AlarmScheduler를 구현한다.
- 체크포인트 11D: Service가 Repository와 AlarmScheduler를 조합하고 최초 Reminder 활성화 시점의 권한 요청 결과 및 사용자 안내를 연결한다.
- 계산 시각이 과거면 Reminder만 저장하고 Alarm을 등록하지 않는다.
- 알림 권한이 없으면 Todo/Reminder를 저장하고 Alarm을 등록하지 않는다.
- Exact 사용 가능 시 Exact, 불가 시 허용된 Inexact Alarm을 사용한다.
- 예약 실패도 저장을 롤백하지 않고 `일정은 저장되었지만 알림을 예약하지 못했습니다.` 상태를 표시한다.
- 날짜·시간·Reminder 수정 시 기존 Alarm을 취소하고 미래 Alarm을 재등록한다.
- TODO 삭제 시 Reminder와 미래 Alarm을 제거한다.
- 완료 시 Reminder는 유지하고 미래 Alarm만 취소하며, 미완료 복귀 시 미래 Reminder만 재등록한다.
- AlarmScheduler는 Repository 내부에 숨기지 않고 TodoService 또는 ReminderService에 별도 abstraction으로 주입한다.

### 테스트 항목

- 15분/1일 전 및 복수 Reminder 계산
- 과거 Reminder 저장 및 Alarm 미등록
- 알림 권한 요청 시점, 거부 시 저장 유지/Alarm 미등록
- Exact 가능/불가와 Inexact fallback
- 등록·취소·수정 후 재등록 및 안정적인 Alarm ID
- 완료 시 취소, 미완료 복귀 시 미래만 재등록
- 예약 실패 시 저장 유지와 사용자 메시지
- Receiver의 Todo 식별 Notification 생성
- 각 체크포인트의 JVM/계측 테스트와 `assembleDebug`

### 완료 조건

- Reminder 데이터와 시스템 Alarm 상태가 확정 정책대로 동작한다.
- 권한 거부와 예약 실패로 앱이 종료되거나 무한 로딩되지 않는다.
- CRUD/필터 회귀 테스트와 알림 관련 테스트가 통과한다.
- 에뮬레이터에서 짧은 미래 시각 Notification을 수동 확인한다.

### 해당 Phase에서 수정하면 안 되는 범위

- 로그인, 네트워크 또는 클라우드 동기화
- 반복·위치 기반·시간 없는 TODO 알림
- 부팅 완료 Receiver와 재부팅 복구
- Category 및 Todo 정렬 정책

## Phase 12: 재부팅 및 Alarm 복구

### 목적

기기 재부팅 후 Room의 유효한 미래 Reminder로 필요한 Alarm만 안전하게 복구한다.

### 구현 대상

- BOOT_COMPLETED Receiver
- 미래 Reminder 재스케줄러
- Manifest 부팅 권한/Receiver
- 중복 방지와 실패 격리

### 생성 또는 수정할 예상 파일

- `app/src/main/java/com/example/todoapplication/notification/BootReceiver.kt`
- `app/src/main/java/com/example/todoapplication/notification/ReminderRescheduler.kt`
- `app/src/main/java/com/example/todoapplication/app/DefaultAppContainer.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/example/todoapplication/notification/ReminderReschedulerTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/notification/BootReceiverTest.kt`

### 의존하는 이전 Phase

- Phase 11

### 구현 상세

- 부팅 완료 이벤트에서 비동기 복구 작업을 안전하게 시작한다.
- 현재보다 미래이고 미완료 TODO에 속한 Reminder만 조회한다.
- 지나간 Reminder는 보존하되 알림을 발생시키지 않는다.
- Phase 11과 같은 Alarm ID 및 Exact/Inexact fallback을 재사용한다.
- 한 Alarm 실패가 나머지 복구를 중단하지 않도록 처리한다.
- 필요한 최소 Manifest 권한과 exported 설정만 선언한다.

### 테스트 항목

- 미래/과거 Reminder 분리
- 완료 Todo Reminder 제외
- 동일 Alarm ID 사용과 중복 방지
- Exact→Inexact fallback 재사용
- 일부 예약 실패 시 나머지 계속 처리
- 에뮬레이터 재부팅 후 미래 Alarm 수동 확인

### 완료 조건

- 재부팅 후 미래·미완료 Reminder만 다시 예약된다.
- 지나간 Reminder가 알림을 발생시키지 않는다.
- Receiver 테스트, 전체 빌드와 기존 회귀 테스트가 통과한다.

### 해당 Phase에서 수정하면 안 되는 범위

- 이미 지난 Reminder 삭제 또는 즉시 알림
- 주기 작업, 서버 동기화와 WorkManager 기능
- CRUD, 필터 및 Category 정책 변경
- 새로운 권한 추가

## Phase 13: 통합 테스트 및 MVP 완료 검증

### 목적

확정된 MVP 제품 완료조건을 자동 테스트와 수동 시나리오로 검증하고 릴리스 가능한 기준선을 만든다.

### 구현 대상

- 주요 사용자 흐름 통합 테스트
- DAO/Repository/Service/Presenter/UI/Alarm 회귀 테스트 정리
- 접근성, 오류, 데이터 유지와 권한 시나리오 검증
- 테스트로 발견된 요구사항 범위 내 결함 수정

### 생성 또는 수정할 예상 파일

- `app/src/androidTest/java/com/example/todoapplication/TodoMvpFlowTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/CategoryMvpFlowTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/ReminderMvpFlowTest.kt`
- 기존 `app/src/test/**` 및 `app/src/androidTest/**` 테스트 파일
- 결함과 직접 관련된 기존 구현 파일
- 필요 시 `app/src/main/res/values/strings.xml`

### 의존하는 이전 Phase

- Phase 0~12 전체

### 구현 상세

- Category 생성·수정·삭제·재정렬·색상 및 `일반` 보호를 검증한다.
- Todo 등록·수정·삭제·완료·날짜 이동·복합 필터와 재실행 영속화를 검증한다.
- Reminder 권한·과거 시각·Exact/Inexact·예약 실패·완료/복귀·재부팅을 검증한다.
- 로딩, 빈 상태, 오류 상태와 입력 유지가 요구사항과 일치하는지 확인한다.
- 접근성 설명, 색상+이름 표시와 다크 테마의 기본 가독성을 확인한다.
- 요구사항 밖 기능은 추가하지 않고 발견된 결함만 수정한다.

### 테스트 항목

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest`
- 앱 프로세스 종료/재실행 후 데이터 및 Category 순서 유지
- 실제 알림 권한 허용/거부 및 미래 Notification
- 에뮬레이터 재부팅 후 Alarm 복구
- 전체 제품 완료조건 체크리스트 수동 확인

### 완료 조건

- 제품 요구사항의 MVP 완료조건을 모두 충족한다.
- 자동 테스트가 통과하고 수동 전용 검증 결과가 기록되어 있다.
- 앱이 minSdk 24 기준으로 빌드 가능하다.
- 알려진 미검증 범위와 실행 환경 제한이 없다.

### 해당 Phase에서 수정하면 안 되는 범위

- 요구사항에 없는 신규 기능
- 대규모 구조 변경과 무관한 리팩터링
- Hilt/Koin, Network DTO 또는 서버 계층 도입
- destructive migration 설정

## Phase 완료 보고 형식

```text
Phase:
변경 파일:
충족한 요구사항:
실행한 빌드/테스트:
자동 테스트 결과:
수동 확인 결과:
남은 제한 사항:
다음 Phase 진행 가능 여부:
```
