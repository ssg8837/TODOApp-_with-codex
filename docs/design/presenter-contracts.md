# Presenter 계약

상태: 확정(공통 계약), 기능별 필드는 구현 시 구체화

## 공통 형태

```kotlin
interface FeaturePresenter {
    val state: StateFlow<FeatureUiState>
    fun onEvent(event: FeatureEvent)
}
```

구현 클래스는 생명주기와 coroutine scope를 위해 AndroidX `ViewModel`을 상속한다. 이 경우에도 역할명과 책임은 Presenter로 유지한다.

## 공통 상태

- 불변 `data class`를 사용한다.
- 화면에 필요한 데이터, 입력값, 로딩 여부와 사용자 표시 오류를 포함한다.
- DB 데이터를 별도의 독립 mutable state로 중복 보관하지 않는다.

## 기능별 Presenter

- `TodoListPresenter`: 날짜와 필터 상태, Service가 제공하는 목록 관찰, 사용자 완료 변경 이벤트 전달 및 결과 표시
- `TodoEditPresenter`: 신규·기존 화면 상태와 입력값 관리, Service 저장 결과 및 검증 오류 표시
- `CategoryPresenter`: 저장 순서의 Category 화면 상태, 색상·재정렬 입력과 Service 결과 표시

## 경계

- View는 Application Service, Repository, DAO와 Room을 직접 호출하지 않는다.
- Presenter는 Application Service만 호출하며 Repository, DAO와 Room을 직접 호출하지 않는다.
- Presenter는 Domain Validator를 직접 호출하거나 비즈니스 규칙을 구현하지 않는다.
- Presenter는 Composable을 호출하거나 `NavController`를 보관하지 않는다.
- 이동과 일회성 메시지는 테스트 가능하며 중복 소비되지 않는 계약으로 전달한다.
- 알람 예약과 데이터 변경의 조정은 Application Service가 `AlarmScheduler` 계약과 Repository를 조합해 수행한다.
- 빈 필드 등 즉각적인 UI 피드백을 위한 보조 검증은 허용하지만 최종 비즈니스 검증은 Application Service가 수행한다.
- 모든 의존성은 constructor로 받고, AndroidX ViewModel 생성에는 명시적인 Factory를 사용한다.
- Alarm 예약 실패는 저장 성공과 구분되는 사용자 안내 상태로 표현한다.
- Category 재정렬 이벤트는 Service에 전달한다. `일반` 이동 거부와 사용자 Category의 연속된 `sortOrder` 구성은 `CategoryService`의 비즈니스 규칙이다.

## Application Service 계약

- `TodoService`: Todo 등록·수정·삭제·완료 변경·날짜별 조회, Todo Validator 적용과 향후 Reminder/Alarm 연계 조정
- `CategoryService`: Category 생성·수정·삭제·순서 변경, 시스템 Category 보호와 Category Validator 적용
- `ReminderService`: Reminder 등록·교체·삭제, Reminder Validator 적용과 향후 AlarmScheduler 연계
- Service는 DB transaction 절차를 재구현하지 않고 Repository의 원자적 연산을 호출한다.
