# TODO App MVP 구현 계획

상태: 구현 전 계획  
기준 문서: `AGENTS.md`, `docs/requirements/`, `docs/rules/`, `docs/design/`

## 1. 진행 원칙

- 아래 단계는 순서대로 누적하며 각 단계 종료 시 빌드 또는 관련 테스트가 통과해야 한다.
- 데이터 계층 → Presenter → Compose UI → 알림 순서로 진행한다.
- Room 데이터를 영속 상태의 Source of Truth로 사용한다.
- 논리 구조는 MVP이며 Presenter 구현은 AndroidX `ViewModel`을 생명주기 기반으로 사용할 수 있다.
- Composable은 상태 렌더링과 이벤트 전달만 담당한다.
- 앱 소스 변경과 함께 직접 관련된 단위 테스트 또는 계측 테스트를 추가한다.
- 한 단계가 실패하면 다음 단계로 넘어가지 않는다.
- 요구사항에 없는 기능은 구현하지 않는다.

## 2. 공통 검증 명령

각 단계의 범위에 맞게 다음 명령을 사용한다.

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

`connectedDebugAndroidTest`는 에뮬레이터 또는 기기를 사용할 수 있는 단계에서 실행한다. 실행 환경이 없으면 미검증 범위를 기록하고, 다음 계측 가능한 시점에 반드시 실행한다.

## 3. 단계별 구현 계획

### 단계 1. 빌드 기반과 의존성 준비

목표: 이후 작은 단계가 동일한 도구 체계에서 컴파일되고 테스트되도록 기반을 준비한다.

구현 대상 파일:

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`

구현 내용:

- Room, Navigation Compose, Lifecycle ViewModel/Compose, Coroutines 및 Room 테스트에 필요한 최소 의존성만 Version Catalog로 추가한다.
- KSP 등 Room 코드 생성에 필요한 플러그인을 프로젝트 방식에 맞게 설정한다.
- 기존 Compose BOM과 SDK 설정을 유지한다.

완료 조건:

- 기존 `Greeting` 앱이 그대로 컴파일된다.
- 단위 및 계측 테스트 소스가 컴파일된다.
- 중복되거나 사용하지 않는 라이브러리가 추가되지 않는다.

테스트 방법:

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest`

### 단계 2. 도메인 모델과 입력 검증

목표: Android UI 및 Room과 독립적인 핵심 데이터와 확정된 검증 규칙을 만든다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/domain/model/Todo.kt`
- `app/src/main/java/com/example/todoapplication/domain/model/Category.kt`
- `app/src/main/java/com/example/todoapplication/domain/model/Reminder.kt`
- `app/src/main/java/com/example/todoapplication/domain/model/ReminderOffset.kt`
- `app/src/main/java/com/example/todoapplication/domain/validation/TodoValidator.kt`
- `app/src/main/java/com/example/todoapplication/domain/validation/CategoryValidator.kt`
- `app/src/test/java/com/example/todoapplication/domain/validation/TodoValidatorTest.kt`
- `app/src/test/java/com/example/todoapplication/domain/validation/CategoryValidatorTest.kt`

구현 내용:

- Todo, Category, Reminder의 도메인 표현을 추가한다.
- 날짜와 시간은 `LocalDate`, `LocalTime?`으로 표현한다.
- 제목·날짜 필수, 공백 제목 금지, 공백 Category 이름 금지, 시간 없는 Reminder 금지를 검증한다.
- 알림 간격 `15`, `1440`을 의미 있는 타입으로 표현한다.

완료 조건:

- 도메인 코드가 Android 및 Compose 타입에 의존하지 않는다.
- 유효한 입력과 각 검증 실패를 명시적으로 구분한다.
- 강제 언래핑과 알림 매직 넘버가 없다.

테스트 방법:

- `./gradlew testDebugUnitTest`
- 제목, 날짜, Category 이름, 시간과 Reminder 조합의 경계값 단위 테스트

### 단계 3. Room Entity와 변환기

목표: 도메인 데이터의 로컬 영속 표현을 독립적으로 검증한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/data/local/entity/TodoEntity.kt`
- `app/src/main/java/com/example/todoapplication/data/local/entity/CategoryEntity.kt`
- `app/src/main/java/com/example/todoapplication/data/local/entity/ReminderEntity.kt`
- `app/src/main/java/com/example/todoapplication/data/local/DateTimeConverters.kt`
- `app/src/main/java/com/example/todoapplication/data/mapper/TodoMapper.kt`
- `app/src/main/java/com/example/todoapplication/data/mapper/CategoryMapper.kt`
- `app/src/main/java/com/example/todoapplication/data/mapper/ReminderMapper.kt`
- `app/src/test/java/com/example/todoapplication/data/mapper/ModelMapperTest.kt`
- `app/src/test/java/com/example/todoapplication/data/local/DateTimeConvertersTest.kt`

구현 내용:

- 날짜를 `epochDay`, 시간을 `minuteOfDay`로 변환한다.
- Todo, Category와 Reminder Entity를 분리한다.
- Todo와 Reminder의 1:N 관계 및 Todo 삭제 시 Reminder 삭제 관계를 표현한다.
- Entity와 도메인 모델 간 mapper를 추가한다.

완료 조건:

- 표시 문자열이 DB 날짜·시간 값으로 사용되지 않는다.
- 도메인↔Entity 변환이 왕복 가능하다.
- Todo와 Reminder 사이에 문서화된 관계만 반영된다.

테스트 방법:

- `./gradlew testDebugUnitTest`
- 날짜·시간 converter 및 mapper 왕복 단위 테스트
- `./gradlew assembleDebug`

### 단계 4. DAO와 Room Database

목표: 앱 UI 없이 CRUD와 날짜·필터 쿼리를 검증할 수 있는 DB를 완성한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/data/local/dao/TodoDao.kt`
- `app/src/main/java/com/example/todoapplication/data/local/dao/CategoryDao.kt`
- `app/src/main/java/com/example/todoapplication/data/local/dao/ReminderDao.kt`
- `app/src/main/java/com/example/todoapplication/data/local/TodoDatabase.kt`
- `app/src/androidTest/java/com/example/todoapplication/data/local/TodoDaoTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/data/local/CategoryDaoTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/data/local/ReminderDaoTest.kt`

구현 내용:

- Todo의 등록·ID 조회·수정·삭제, Category의 등록·조회·이름 수정, Reminder의 등록·조회·삭제 DAO를 구현한다.
- Todo의 날짜, 선택 Category 및 미완료 조건을 AND로 적용하는 관찰 쿼리를 제공한다.
- Todo 삭제 시 연결 Reminder가 함께 삭제되는지 검증한다.
- 목록 관찰 결과는 `Flow`로 제공한다.

완료 조건:

- in-memory Room DB에서 확정된 CRUD와 복합 필터가 동작한다.
- DB 스키마가 컴파일되고 앱 프로세스 재시작에 사용할 영속 DB 생성이 가능하다.
- 문서에 확정된 DAO 동작이 각각 독립적으로 검증된다.

테스트 방법:

- `./gradlew assembleDebug`
- `./gradlew connectedDebugAndroidTest`

### 단계 5. Repository 계층

목표: Presenter가 Room 세부 구현을 모른 채 데이터를 사용할 수 있게 한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/domain/repository/TodoRepository.kt`
- `app/src/main/java/com/example/todoapplication/domain/repository/CategoryRepository.kt`
- `app/src/main/java/com/example/todoapplication/domain/repository/ReminderRepository.kt`
- `app/src/main/java/com/example/todoapplication/data/repository/RoomTodoRepository.kt`
- `app/src/main/java/com/example/todoapplication/data/repository/RoomCategoryRepository.kt`
- `app/src/main/java/com/example/todoapplication/data/repository/RoomReminderRepository.kt`
- `app/src/test/java/com/example/todoapplication/data/repository/RepositoryTest.kt`

구현 내용:

- DAO를 감싼 도메인 중심 Repository 계약과 구현을 추가한다.
- Todo 저장·수정·삭제 및 필터 관찰을 제공한다.
- Category 생성·조회·이름 수정과 Reminder 저장·조회를 제공한다.
- 저장 실패를 Presenter가 처리 가능한 결과 또는 예외 경계로 전달한다.

완료 조건:

- Presenter가 Entity나 DAO 타입에 의존할 필요가 없다.
- mapper와 transaction 경계가 Repository 내부에 있다.
- fake DAO 또는 fake Repository 기반 테스트가 통과한다.

테스트 방법:

- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest`

### 단계 6. Presenter 공통 계약과 테스트 도구

목표: 기능별 Presenter를 동일한 상태·이벤트 패턴으로 구현할 기반을 만든다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/feature/common/UiMessage.kt`
- `app/src/test/java/com/example/todoapplication/test/FakeTodoRepository.kt`
- `app/src/test/java/com/example/todoapplication/test/FakeCategoryRepository.kt`
- `app/src/test/java/com/example/todoapplication/test/MainDispatcherRule.kt`

구현 내용:

- 불변 `UiState`, 이벤트 처리와 사용자 메시지 전달 원칙을 구현 가능한 공통 형태로 정리한다.
- 테스트에서 시간과 coroutine dispatcher를 제어할 수 있게 한다.
- Android UI 없이 Presenter를 검증할 fake 저장소를 만든다.

완료 조건:

- Presenter 테스트가 실제 Room, Activity와 시스템 시간 없이 실행 가능하다.
- 사용자 메시지와 내부 오류가 분리된다.

테스트 방법:

- 테스트 도구 자체의 작은 검증 테스트
- `./gradlew testDebugUnitTest`

### 단계 7. Category Presenter

목표: Category 조회·생성·이름 수정을 UI 없이 완성한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/feature/category/CategoryUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryEvent.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryPresenter.kt`
- `app/src/test/java/com/example/todoapplication/feature/category/CategoryPresenterTest.kt`

구현 내용:

- Category 목록을 `StateFlow` 상태로 제공한다.
- 이름 입력, 검증, 생성과 이름 수정을 처리한다.
- 저장 중 중복 요청을 막고 실패 시 입력 또는 기존 값을 유지한다.

완료 조건:

- 빈 이름과 공백 이름 저장이 차단된다.
- 성공, 실패, 빈 목록과 중복 저장 상태 전이가 검증된다.
- Presenter가 Compose 및 Room 타입에 의존하지 않는다.

테스트 방법:

- `./gradlew testDebugUnitTest`

### 단계 8. Todo 목록 Presenter와 필터

목표: 날짜 조회, 완료 상태 및 모든 확정 필터 로직을 UI 없이 안정화한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListEvent.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListPresenter.kt`
- `app/src/test/java/com/example/todoapplication/feature/todo/list/TodoListPresenterTest.kt`

구현 내용:

- 최초 날짜를 오늘로 설정하고 이전·다음 날짜 이벤트를 처리한다.
- Category와 미완료 필터를 날짜와 AND로 결합한다.
- 완료·미완료 변경을 즉시 반영하고 저장 실패 시 일관된 상태로 복구한다.
- 일반 로컬 조회에는 불필요한 전체 화면 로딩을 표시하지 않는다.

완료 조건:

- 날짜 이동, 전체 Category, 특정 Category와 미완료 조합이 검증된다.
- 날짜 빈 상태와 필터 결과 없음 상태가 구분된다.
- 조회 및 완료 저장 실패가 사용자 메시지로 변환된다.

테스트 방법:

- 고정 Clock을 사용한 오늘 날짜 테스트
- 상태 전이 및 복합 필터 단위 테스트
- `./gradlew testDebugUnitTest`

### 단계 9. Todo 등록·수정 Presenter

목표: 알림을 제외한 Todo 입력, 등록, 수정과 삭제 흐름을 UI 없이 완성한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditEvent.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditPresenter.kt`
- `app/src/test/java/com/example/todoapplication/feature/todo/edit/TodoEditPresenterTest.kt`

구현 내용:

- 신규 입력과 Todo ID 기반 수정 데이터 로드를 지원한다.
- 제목, 날짜, 선택 시간과 선택 Category 입력을 처리한다.
- 저장 직전 검증, 중복 저장 방지와 실패 시 입력값 유지를 구현한다.
- 명시적 확인 이후 Todo 삭제를 실행하고 실패 시 기존 상태를 유지한다.

완료 조건:

- 신규 저장, 기존 조회·수정, 존재하지 않는 ID와 삭제 실패가 검증된다.
- 성공 시 목록으로 돌아갈 수 있는 일회성 결과가 중복 소비되지 않는다.
- Reminder와 시스템 알람 코드는 아직 연결되지 않는다.

테스트 방법:

- 검증 실패, 저장 성공·실패, 수정 로드, 삭제 성공·실패 단위 테스트
- `./gradlew testDebugUnitTest`

### 단계 10. Compose 앱 셸과 Navigation

목표: 세 화면의 빈 골격을 연결하고 각 화면을 독립적으로 렌더링 가능하게 한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/MainActivity.kt`
- `app/src/main/java/com/example/todoapplication/app/TodoApp.kt`
- `app/src/main/java/com/example/todoapplication/navigation/Destination.kt`
- `app/src/main/java/com/example/todoapplication/navigation/TodoNavHost.kt`
- `app/src/main/java/com/example/todoapplication/app/AppContainer.kt`
- `app/src/androidTest/java/com/example/todoapplication/navigation/TodoNavigationTest.kt`

구현 내용:

- `TodoList`, `TodoEdit`, `CategoryManagement` 목적지를 구성한다.
- 수정 화면에는 전체 객체가 아닌 Todo ID만 전달한다.
- 앱 범위 DB와 Repository를 작은 수동 조립으로 제공한다.
- 초기 `Greeting`을 앱 셸로 교체하되 기능 UI는 다음 단계에서 채운다.

완료 조건:

- 앱 시작 시 TodoList 목적지가 표시된다.
- 세 목적지 간 기본 이동과 뒤로가기가 동작한다.
- Activity에 비즈니스 로직이 없다.

테스트 방법:

- 각 목적지 진입 및 뒤로가기 Compose UI 테스트
- `./gradlew assembleDebug`
- `./gradlew connectedDebugAndroidTest`

### 단계 11. Category 관리 UI

목표: 확정된 Category 조회·생성·이름 수정 기능을 화면에서 완성한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/feature/category/CategoryRoute.kt`
- `app/src/main/java/com/example/todoapplication/feature/category/CategoryScreen.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/EmptyState.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/example/todoapplication/feature/category/CategoryScreenTest.kt`

구현 내용:

- 목록, 종류 없음 안내, 이름 입력, 생성과 이름 수정 UI를 구현한다.
- 검증 오류를 입력 가까이에 표시하고 저장 실패 시 입력값을 유지한다.
- 아이콘 버튼에 접근성 설명을 제공한다.

완료 조건:

- Category 생성과 이름 수정 결과가 Room에서 다시 관찰된다.
- 종류 없음, 검증 오류와 저장 실패 상태가 렌더링된다.
- 사용자 노출 문자열이 리소스로 관리된다.

테스트 방법:

- Preview로 주요 상태 확인
- 생성·수정·빈 상태 Compose UI 테스트
- `./gradlew connectedDebugAndroidTest`

### 단계 12. Todo 목록 UI와 필터 UI

목표: 날짜별 목록, 완료 변경과 복합 필터를 실제 화면에서 제공한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListRoute.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoListScreen.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/list/TodoItem.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/ErrorMessage.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/example/todoapplication/feature/todo/list/TodoListScreenTest.kt`

구현 내용:

- 오늘, 이전·다음 날짜, TODO 목록과 완료 토글을 표시한다.
- 전체/특정 Category와 미완료 필터 UI를 제공한다.
- 날짜 빈 상태와 필터 결과 없음 상태를 구분한다.
- 추가, 수정 및 Category 관리 이동 이벤트를 연결한다.

완료 조건:

- 날짜 이동과 필터 조합에 맞는 항목만 표시된다.
- 완료 처리 시 화면이 갱신되고 미완료 필터 결과에서 제외된다.
- 오류와 빈 상태가 접근 가능하게 표시된다.

테스트 방법:

- 화면 상태별 Preview
- 날짜 이동, 완료 변경, Category 및 미완료 복합 필터 UI 테스트
- `./gradlew connectedDebugAndroidTest`

### 단계 13. Todo 등록·수정 UI와 기본 CRUD 안정화

목표: 알림을 제외한 TODO CRUD와 Category 선택을 end-to-end로 안정화한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditRoute.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditScreen.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/TodoDateField.kt`
- `app/src/main/java/com/example/todoapplication/ui/component/TodoTimeField.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/example/todoapplication/feature/todo/edit/TodoEditScreenTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/TodoCrudFlowTest.kt`

구현 내용:

- 제목, 날짜, 선택 시간과 선택 Category 입력 UI를 제공한다.
- 신규 등록과 Todo ID 기반 수정 데이터를 같은 화면 흐름에 연결한다.
- 확인 후 TODO 삭제, 검증 메시지, 저장 중 중복 방지를 구현한다.
- Room 저장 후 목록에 반영되고 프로세스 재생성에도 유지되는지 확인한다.

완료 조건:

- TODO 등록·조회·수정·삭제 및 완료 변경이 통합 동작한다.
- 날짜 및 복합 필터가 CRUD 결과와 일관되게 동작한다.
- 기본 CRUD/필터 관련 단위·DAO·UI 테스트가 모두 통과한다.

테스트 방법:

- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest`
- 앱 재실행 후 저장 데이터 유지 수동 확인

### 단계 14. Reminder 저장과 시간 계산

선행 조건: 단계 13의 기본 CRUD와 필터 테스트가 안정적으로 통과해야 한다.

목표: Android 시스템 알람과 분리하여 Reminder 입력·저장·계산을 먼저 완성한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/domain/reminder/ReminderCalculator.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditUiState.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditEvent.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditPresenter.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditScreen.kt`
- `app/src/test/java/com/example/todoapplication/domain/reminder/ReminderCalculatorTest.kt`
- `app/src/test/java/com/example/todoapplication/feature/todo/edit/TodoEditReminderTest.kt`

구현 내용:

- 시간이 있을 때 `15분 전`, `1일 전`을 각각 또는 동시에 선택한다.
- 시간이 없으면 Reminder 선택과 저장을 차단하고 안내한다.
- Todo와 Reminder DB 변경을 일관된 저장 흐름으로 처리한다.
- 날짜, 시간 또는 Reminder 수정 시 필요한 스케줄 변경 정보를 산출한다.

완료 조건:

- Reminder 선택값이 DB에 저장되고 수정 시 교체된다.
- 알림 시각 계산과 복수 Reminder가 단위 테스트된다.
- 아직 AlarmManager를 호출하지 않아도 전체 앱 빌드와 CRUD가 정상이다.

테스트 방법:

- `./gradlew testDebugUnitTest`
- Reminder DAO 계측 테스트
- `./gradlew connectedDebugAndroidTest`

### 단계 15. 시스템 알람과 Notification

목표: 저장된 미래 Reminder를 Android 알람과 알림으로 연결한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/notification/AlarmScheduler.kt`
- `app/src/main/java/com/example/todoapplication/notification/AndroidAlarmScheduler.kt`
- `app/src/main/java/com/example/todoapplication/notification/TodoAlarmReceiver.kt`
- `app/src/main/java/com/example/todoapplication/notification/NotificationChannels.kt`
- `app/src/main/java/com/example/todoapplication/notification/NotificationFactory.kt`
- `app/src/main/java/com/example/todoapplication/feature/todo/edit/TodoEditPresenter.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/example/todoapplication/notification/AlarmIdentityTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/notification/AlarmSchedulerTest.kt`

구현 내용:

- Todo와 Reminder를 식별하는 안정적인 알람 ID를 생성한다.
- 저장 시 등록하고 날짜·시간·Reminder 수정 시 취소 후 재등록한다.
- TODO 삭제 시 관련 미래 알람을 취소한다.
- TODO 알림용 Notification Channel을 생성하고 Receiver에서 알림을 표시한다.
- Android 13 이상 알림 권한 상태를 처리하되 TODO 저장을 막지 않는다.

완료 조건:

- 알람 등록·취소·수정 후 재등록이 검증된다.
- 알림 권한이 없어도 앱과 TODO 저장은 정상 동작한다.
- 예약 시각에 Todo를 식별할 수 있는 Notification이 표시된다.
- 필요한 권한과 Receiver만 Manifest에 선언된다.

테스트 방법:

- fake `AlarmScheduler`를 사용한 Presenter 단위 테스트
- 알람 ID 및 PendingIntent 일치 테스트
- 짧은 미래 시각을 사용한 에뮬레이터 수동 알림 확인
- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest`

### 단계 16. 재부팅 복구와 MVP 회귀 검증

목표: 저장된 미래 Reminder 복구와 전체 확정 범위의 회귀 검증을 완료한다.

구현 대상 파일:

- `app/src/main/java/com/example/todoapplication/notification/BootReceiver.kt`
- `app/src/main/java/com/example/todoapplication/notification/ReminderRescheduler.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/example/todoapplication/notification/ReminderReschedulerTest.kt`
- `app/src/androidTest/java/com/example/todoapplication/TodoMvpFlowTest.kt`

구현 내용:

- 부팅 완료 후 Room의 유효한 미래 Reminder를 읽어 재등록한다.
- 중복 등록 없이 동일한 알람 ID 규칙을 적용한다.
- CRUD, 재실행 영속화, 날짜 이동, 필터, Reminder 및 알림의 회귀 테스트를 정리한다.

완료 조건:

- 재부팅 후 유효한 미래 알람이 복구된다.
- 앱 재실행 후 Todo, Category와 Reminder가 유지된다.
- 확정된 MVP 시나리오의 자동 테스트와 수동 알림 확인이 통과한다.
- 미실행 테스트나 남은 제한 사항이 결과 보고서에 명시된다.

테스트 방법:

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest`
- 에뮬레이터 재부팅 후 미래 알람 재등록 수동 확인

## 4. 단계 완료 보고 형식

각 단계 완료 시 다음 항목을 보고한다.

```text
단계:
변경 파일:
충족한 요구사항:
실행한 빌드/테스트:
테스트 결과:
수동 확인:
남은 제한 사항:
다음 단계 진행 가능 여부:
```
