# 기술 요구사항 (TRD)

상태: 초안

## 1. 기술 목표

TODO App은 Android 네이티브 애플리케이션으로 구현한다.

기술적으로 다음을 우선한다.

- 학습하기 쉬운 구조
- 화면과 데이터 처리의 책임 분리
- 테스트 가능한 구조
- 로컬 우선(Local-first) 데이터 관리
- 알림 기능의 신뢰성
- 요구사항 변경에 대응 가능한 확장성
- 불필요한 프레임워크 및 복잡성 최소화

---

## 2. 개발 환경

### 개발 장비
- Mac Apple Silicon(M1)

### 개발 도구
- Android Studio
- VS Code
- OpenAI Codex
- Android Emulator

### 언어
- Kotlin

### UI
- Jetpack Compose

### 빌드
- Gradle Wrapper

프로젝트에 포함된 Gradle Wrapper를 사용한다.

전역 Gradle 설치를 프로젝트 요구사항으로 두지 않는다.

---

## 3. 기술 스택

| 영역 | 기술 |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | UI + ViewModel + Repository + Data Source |
| UI State | StateFlow |
| Local DB | Room |
| Navigation | Navigation Compose |
| Notification | Android Notification API |
| Alarm Scheduling | AlarmManager 기반 |
| Async | Kotlin Coroutines |
| Build | Gradle |
| Test | JUnit / AndroidX Test / Compose UI Test |

라이브러리 버전은 문서에 고정하지 않고 프로젝트의 Version Catalog 또는 Gradle 설정에서 관리한다.

---

## 4. 애플리케이션 구조

기본 구조는 다음 책임 분리를 따른다.

```text
UI (Compose)
    ↓
ViewModel
    ↓
Repository
    ↓
Room / Android System APIs
```

### UI
담당:
- 화면 표시
- 사용자 입력 전달
- UI State 표시
- 로딩/빈 상태/오류 표시

UI에서 직접 데이터베이스에 접근하지 않는다.

### ViewModel
담당:
- 화면 상태 관리
- 사용자 이벤트 처리
- Repository 호출
- UI용 데이터 가공
- 입력 검증 결과 반영

화면 상태는 가능한 한 불변(immutable) `UiState`로 표현한다.

### Repository
담당:
- 데이터 접근 인터페이스 제공
- Room 접근 캡슐화
- ViewModel이 저장소 구현 세부사항을 알지 못하도록 분리

### Data Source
MVP에서는 로컬 데이터만 사용한다.

주요 데이터 소스:
- Room Database
- AlarmManager
- NotificationManager

---

## 5. 권장 패키지 구조

```text
com.example.todoapp
├── data
│   ├── local
│   │   ├── dao
│   │   ├── entity
│   │   └── TodoDatabase.kt
│   ├── repository
│   └── mapper
│
├── domain
│   └── model
│
├── ui
│   ├── todo
│   │   ├── list
│   │   └── edit
│   ├── category
│   ├── navigation
│   └── component
│
├── notification
│   ├── AlarmScheduler.kt
│   ├── TodoAlarmReceiver.kt
│   └── BootReceiver.kt
│
└── MainActivity.kt
```

초기 구현에서 불필요한 추상화 계층은 추가하지 않는다.

---

## 6. 데이터 모델

### 6.1 Todo

논리 모델:

```text
Todo
- id
- title
- date
- time
- categoryId
- isCompleted
- createdAt
- updatedAt
```

요구사항:

- `id`는 TODO를 식별하는 고유값이다.
- `title`은 필수이다.
- `date`는 필수이다.
- `time`은 nullable이다.
- `categoryId`는 카테고리 정책 확정 전까지 nullable을 허용할 수 있는 구조로 설계한다.
- `isCompleted` 기본값은 false이다.

### 6.2 Category

```text
Category
- id
- name
- createdAt
```

요구사항:

- `id`는 고유값이다.
- `name`은 빈 문자열을 허용하지 않는다.
- 카테고리 이름 중복 허용 여부는 상세 요구사항에서 결정한다.

### 6.3 Reminder

```text
Reminder
- id
- todoId
- minutesBefore
```

예:

```text
15분 전  -> 15
1일 전   -> 1440
```

하나의 TODO가 복수 Reminder를 가질 수 있도록 별도 테이블로 관리한다.

---

## 7. 데이터 관계

```text
Category 1 ---- N Todo
Todo     1 ---- N Reminder
```

### 외래키 정책

- Reminder는 반드시 유효한 Todo와 연결되어야 한다.
- Todo 삭제 시 연결된 Reminder도 함께 삭제한다.
- Category 삭제 시 Todo 처리 정책은 제품 요구사항 확정 후 DB 정책을 결정한다.

카테고리 삭제 정책이 확정되기 전에는 `CASCADE DELETE`를 임의로 적용하지 않는다.

---

## 8. 데이터 타입 정책

날짜/시간은 화면 표시 문자열을 그대로 DB에 저장하지 않는다.

권장 논리 타입:

```text
Todo.date -> LocalDate
Todo.time -> LocalTime?
```

Room 저장 시 TypeConverter 또는 명확한 primitive 변환 규칙을 사용한다.

예:
- LocalDate → epochDay
- LocalTime → minuteOfDay

표시 형식은 UI 계층에서 처리한다.

날짜 비교 및 필터에서 문자열 포맷에 의존하지 않는다.

---

## 9. Room 요구사항

Room은 다음 데이터를 영속화한다.

- Todo
- Category
- Reminder

필수 DAO 기능:

### TodoDao
- TODO 등록
- TODO 수정
- TODO 삭제
- ID 조회
- 특정 날짜 TODO 조회
- 완료 여부 필터 조회
- 종류 필터 조회
- 날짜 + 종류 + 완료 여부 복합 조회

### CategoryDao
- 종류 등록
- 종류 수정
- 종류 삭제
- 전체 종류 조회
- ID 조회

### ReminderDao
- Reminder 등록
- Todo별 Reminder 조회
- Reminder 삭제
- Todo에 연결된 Reminder 전체 삭제

DAO 결과 중 화면 목록에 사용되는 데이터는 가능한 경우 `Flow` 기반으로 제공한다.

---

## 10. UI State 관리

화면 상태는 ViewModel에서 관리한다.

예:

```text
TodoListUiState
- selectedDate
- todos
- categories
- selectedCategoryId
- incompleteOnly
- isLoading
- errorMessage
```

Compose 화면은 ViewModel의 StateFlow를 관찰한다.

UI에서 데이터베이스 상태를 별도의 mutable state로 중복 보관하지 않는다.

---

## 11. Navigation

MVP의 주요 목적지는 다음과 같다.

```text
TodoList
TodoEdit
CategoryManagement
```

### TodoList
- 기본 시작 화면

### TodoEdit
등록:
- 신규 TODO 작성

수정:
- TODO ID를 이용해 기존 데이터를 조회하여 편집

### CategoryManagement
- 종류 추가/수정/삭제

화면 간 전달 데이터는 최소화한다.

가능하면 전체 객체 대신 ID를 전달하고 대상 ViewModel에서 저장소를 통해 다시 조회한다.

---

## 12. 알림 설계

### 12.1 기본 정책

시간이 지정된 TODO만 알림을 설정할 수 있다.

초기 옵션:
- 1일 전 (`1440`분)
- 15분 전 (`15`분)

실제 알림 시간:

```text
TODO 예정 일시 - minutesBefore
```

### 12.2 알림 스케줄링

제품 요구사항이 특정 시점의 사용자 알림을 요구하므로 `AlarmManager` 기반 스케줄링을 기본 설계로 한다.

알람 발생 시 `BroadcastReceiver`가 Android Notification을 생성한다.

구조:

```text
Todo 저장
   ↓
Reminder 계산
   ↓
AlarmScheduler
   ↓
AlarmManager
   ↓
TodoAlarmReceiver
   ↓
NotificationManager
```

### 12.3 정확한 알람 권한

최신 Android에서는 정확한 알람 사용에 별도 시스템 정책 및 권한이 적용될 수 있다.

구현 시 다음을 만족해야 한다.

- 정확한 알람 사용 가능 여부를 확인한다.
- 필요한 권한/특수 접근이 없는 경우 앱이 비정상 종료되어서는 안 된다.
- 정확한 알람을 사용할 수 없는 경우 사용자에게 상태를 안내하거나 허용된 대체 동작을 정의한다.
- 권한 정책을 우회하는 구현을 하지 않는다.

정확한 알람이 반드시 필요한지, 일정 범위의 지연을 허용할지는 상세 요구사항에서 최종 확정한다.

### 12.4 알림 권한

Android 13(API 33) 이상에서는 알림 표시를 위한 런타임 권한을 고려한다.

앱은 알림 설정이 실제로 필요한 시점에 권한을 요청한다.

권한이 거부된 경우:
- TODO 저장 자체는 허용한다.
- 알림을 표시할 수 없음을 사용자에게 안내한다.
- 앱이 비정상 종료되어서는 안 된다.

### 12.5 Notification Channel

앱 최초 알림 기능 초기화 시 Notification Channel을 생성한다.

MVP에서는 TODO 알림용 하나의 기본 채널을 사용한다.

---

## 13. 알림 갱신 및 취소

다음 경우 기존 알람을 취소하고 필요한 알람을 다시 계산한다.

- TODO 날짜 변경
- TODO 시간 변경
- Reminder 변경
- TODO 삭제

TODO 완료 시 미래 알림을 취소할지 여부는 상세 요구사항 확정 후 구현한다.

각 알람은 TODO와 Reminder를 식별할 수 있는 안정적인 고유 식별자를 가져야 한다.

---

## 14. 기기 재부팅 대응

Android 시스템 재부팅 시 기존 AlarmManager 알람이 유지되지 않을 수 있으므로 저장된 Reminder를 기준으로 재등록할 수 있는 구조를 둔다.

구현 후보:
- `BOOT_COMPLETED` 수신
- Room에서 아직 유효한 TODO/Reminder 조회
- 미래 알림 재스케줄링

재부팅 직후 이미 지난 알림을 어떻게 처리할지는 상세 요구사항에서 결정한다.

---

## 15. 권한 및 Manifest

필요 가능성이 있는 항목:

- 알림 권한
- 정확한 알람 관련 권한/특수 접근
- 기기 부팅 완료 이벤트 수신

실제 Manifest 선언은 사용 기능과 지원 Android 버전에 따라 최소한으로 추가한다.

사용하지 않는 권한은 선언하지 않는다.

---

## 16. 입력 검증

ViewModel 또는 별도의 검증 로직에서 처리한다.

필수 규칙:

- 제목이 비어 있으면 저장 불가
- 날짜가 없으면 저장 불가
- 시간이 없는 TODO에 Reminder 저장 불가
- 잘못된 Reminder 값 저장 불가
- 저장 실패 시 기존 화면 상태를 잃지 않아야 함

UI 검증만 신뢰하지 않고 저장 직전에도 요구사항에 맞는 상태인지 확인한다.

---

## 17. 오류 처리

원칙:
- 내부 Exception 메시지를 사용자에게 그대로 표시하지 않는다.
- 사용자에게는 행동 가능한 메시지를 제공한다.
- 개발 중에는 원인 확인을 위한 로그를 남길 수 있다.

오류 범주:
- 입력 오류
- DB 저장 오류
- DB 조회 오류
- 알림 권한 오류
- Alarm scheduling 오류

---

## 18. 로딩 및 비동기 처리

- DB 작업은 UI Thread를 장시간 차단하지 않는다.
- Kotlin Coroutine을 사용한다.
- 로컬 조회가 즉시 완료되는 일반 목록 화면에서 불필요한 전체 화면 Spinner를 사용하지 않는다.
- 저장 중 중복 요청이 실제 문제를 일으킬 수 있는 경우 중복 입력을 방지한다.

---

## 19. 테스트 요구사항

### Unit Test
우선 대상:
- TODO 입력 검증
- Reminder 시간 계산
- 필터 조건
- ViewModel 상태 변경

### DAO Test
검증:
- 등록
- 수정
- 삭제
- 날짜 조회
- 종류 조회
- 완료 여부 조회
- 복합 필터
- 관계 삭제

### UI Test
핵심 사용자 흐름:
- TODO 등록
- TODO 완료
- 날짜 변경
- 필터 적용

### 알림 Test
검증:
- Alarm 등록
- Alarm 취소
- TODO 수정 후 재등록
- 알림 권한 미허용
- 이미 지난 Alarm 처리

---

## 20. 개발 규칙

- 요구사항에 없는 기능을 임의로 추가하지 않는다.
- 화면에서 Room DAO를 직접 호출하지 않는다.
- Activity에 비즈니스 로직을 작성하지 않는다.
- Composable에 영속화 로직을 작성하지 않는다.
- DB Entity와 UI 표시 모델을 무조건 동일한 객체로 취급하지 않는다.
- 날짜/시간 로직은 한 곳에서 일관되게 처리한다.
- Magic Number 대신 알림 값에 의미를 부여한다.
- 프로젝트에서 사용하는 의존성 버전은 한 곳에서 관리한다.
- 새로운 라이브러리 도입 전 기존 Android/Kotlin API로 해결 가능한지 확인한다.

---

## 21. 초기 구현 순서

```text
1. 프로젝트 구조 정리
2. Domain/Data Model 정의
3. Room Entity / DAO / Database 구현
4. Repository 구현
5. TODO 목록 ViewModel
6. TODO 목록 UI
7. TODO 등록/수정
8. 종류 관리
9. 필터 기능
10. Notification Channel
11. Alarm Scheduler
12. 알림 Receiver
13. 권한 처리
14. 재부팅 후 알림 복구
15. 테스트 및 수정
```

---

## 22. 기술적 미확정 사항

구현 전에 결정한다.

- 정확한 알람이 필수인지, 수 분 정도의 오차를 허용할지
- 정확한 알람 권한이 없는 경우의 fallback 정책
- 완료 처리 시 미래 Reminder 취소 여부
- 과거 TODO/Reminder 저장 정책
- Category 삭제 시 FK 정책
- Dependency Injection 라이브러리 사용 여부
- Domain model과 Entity를 별도 객체로 분리할 범위
- 최소 지원 Android API Level
