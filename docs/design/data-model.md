# 데이터 모델 및 Room 설계

상태: 확정

## 공통 타입

- Domain ID는 `Long`을 사용한다.
- Room PK는 `Long` 기반 auto-generated ID를 사용한다.
- Domain의 `createdAt`, `updatedAt`은 `Instant`다.
- DB timestamp는 epoch milliseconds 기반 `Long`으로 저장한다.
- 날짜와 시간은 Domain에서 `LocalDate`, `LocalTime?`을 사용하고 DB에는 `epochDay`, `minuteOfDay`로 변환한다.
- 화면 표시 문자열을 DB의 날짜·시간 비교 값으로 사용하지 않는다.

## 논리 모델

```text
Todo
- id: Long
- title: String
- date: LocalDate
- time: LocalTime?
- categoryId: Long (NOT NULL)
- isCompleted: Boolean = false
- createdAt: Instant
- updatedAt: Instant

Category
- id: Long
- name: String (UNIQUE, NOT NULL)
- color: CategoryColor
- sortOrder: Int
- isSystem: Boolean
- createdAt: Instant

Reminder
- id: Long
- todoId: Long
- minutesBefore: 15 또는 1440, 확장 가능
```

## Category 정책

- 모든 Todo는 정확히 하나의 Category를 가지며 `Todo.categoryId`는 NOT NULL이다.
- 최초 DB 생성 시 시스템 기본 Category `일반`을 생성한다.
- `일반`은 사용자 생성 Category가 아니며 이름 수정과 삭제가 불가능하다.
- 사용자 Category 이름은 빈 값과 공백만 있는 값을 허용하지 않는다.
- 시스템 Category를 포함해 Category 이름은 중복될 수 없다.
- Todo 저장 시 Category가 선택되지 않았으면 `일반` ID를 적용한다.
- `일반`은 `color = NEUTRAL`, `sortOrder = 0`, `isSystem = true`로 유지한다.
- 사용자 Category는 `sortOrder = 1`부터 관리하며 새 항목은 현재 마지막에 추가한다.
- Category 조회는 `sortOrder` 오름차순을 사용한다.

## CategoryColor

Domain은 Compose `Color`가 아닌 안정적인 의미 값을 사용한다.

```text
enum class CategoryColor {
    NEUTRAL,
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    BLUE,
    PURPLE,
    PINK
}
```

- Room에는 enum의 안정적인 문자열 값을 저장하고 Compose `Color` 객체를 저장하지 않는다.
- UI 계층이 각 의미 값에 실제 Material 색상을 매핑한다.
- 여러 Category가 같은 색상 값을 가질 수 있다.

## 관계와 삭제 transaction

```text
Category 1 ---- N Todo
Todo     1 ---- N Reminder
```

- Reminder는 반드시 유효한 Todo를 참조한다.
- Todo 삭제 시 연결 Reminder를 함께 삭제한다.
- 사용자 Category 삭제 시 하나의 Room transaction 안에서 다음 순서를 지킨다.
  1. 삭제 대상 Category를 사용하는 모든 Todo의 `categoryId`를 `일반` ID로 갱신한다.
  2. 모든 갱신이 성공한 뒤 대상 Category를 삭제한다.
- Category 삭제로 Todo를 삭제하지 않는다.
- transaction 일부가 실패하면 Category 재지정과 삭제를 모두 반영하지 않는다.

## TODO 정렬

조회 결과는 다음 순서를 보장한다.

1. 시간이 있는 TODO
2. 시간 오름차순
3. 시간이 없는 TODO
4. 같은 시간은 `createdAt` 오름차순

완료 여부는 정렬에 영향을 주지 않는다.

## DAO 계약

### TodoDao

- 등록, 수정, 삭제, ID 조회
- 특정 날짜 조회
- 날짜 + 선택 Category + 선택 미완료 조건의 AND 복합 조회
- Category ID 일괄 변경
- 위 정렬 규칙을 적용한 `Flow` 목록 반환

### CategoryDao

- 등록, 이름 수정, 삭제, 전체 조회, ID 조회와 이름 조회
- 시스템 기본 Category 조회
- 최초 DB 생성 시 `일반` 생성
- 사용자 Category 삭제와 Todo 재지정을 하나의 transaction으로 제공
- `sortOrder` 오름차순 조회 및 사용자 Category 순서 일괄 갱신
- Category 재정렬은 하나의 transaction에서 `1`부터 일관된 순서로 저장

### ReminderDao

- 등록, Todo별 조회, 개별 삭제와 Todo별 전체 삭제
- 현재 시각보다 미래인 Reminder 조회
- 재부팅 복구용 미래 Reminder 조회

## 모델 계층

- Room Entity와 Domain Model을 분리한다.
- Entity↔Domain mapper를 둔다.
- 서버 기능이 없으므로 Network DTO를 만들지 않는다.
- UI 전용 모델은 실제 화면 요구가 있을 때만 추가한다.
- 의미 없는 중간 모델과 변환 계층을 추가하지 않는다.

## Room version과 Migration

- 초기 Database version은 `1`이다.
- schema version을 올릴 때 명시적인 `Migration`을 작성한다.
- `fallbackToDestructiveMigration` 계열을 기본 정책으로 사용하지 않는다.
- 개발 중 DB 초기화가 필요하면 앱 데이터를 명시적으로 삭제한다.
