# Presenter 계약

상태: 확정(공통 계약), 기능별 필드는 구현 시 구체화

## 공통 형태

```kotlin
interface FeaturePresenter {
    val state: StateFlow<FeatureUiState>
    fun onEvent(event: FeatureEvent)
}
```

구현 클래스는 생명주기와 coroutine scope를 위해 AndroidX `ViewModel`을 상속할 수 있다. 이 경우에도 역할명과 책임은 Presenter로 유지한다.

## 공통 상태

- 불변 `data class`를 사용한다.
- 화면에 필요한 데이터, 입력값, 로딩 여부와 사용자 표시 오류를 포함한다.
- DB 데이터를 별도의 독립 mutable state로 중복 보관하지 않는다.

## 기능별 Presenter

- `TodoListPresenter`: 날짜와 필터 상태, 정렬된 목록 관찰, 완료 변경과 미래 알람 취소·복원
- `TodoEditPresenter`: 신규·기존 데이터 로드, 기본 Category 적용, 입력 검증, 저장 및 알람 갱신
- `CategoryPresenter`: 저장 순서의 시스템/사용자 종류 목록 관찰, 중복 검증, 색상 선택, 사용자 종류 생성·수정·transaction 삭제·재정렬

## 경계

- View는 Repository를 직접 호출하지 않는다.
- Presenter는 Composable을 호출하거나 `NavController`를 보관하지 않는다.
- 이동과 일회성 메시지는 테스트 가능하며 중복 소비되지 않는 계약으로 전달한다.
- 알람 예약은 `AlarmScheduler` 계약을 통해 수행한다.
- 입력 검증은 UI 표시뿐 아니라 저장 직전에도 수행한다.
- 모든 의존성은 constructor로 받고, AndroidX ViewModel 생성에는 명시적인 Factory를 사용한다.
- Alarm 예약 실패는 저장 성공과 구분되는 사용자 안내 상태로 표현한다.
- Category 재정렬 이벤트는 `일반` 이동을 거부하고 사용자 Category의 `sortOrder`를 `1`부터 다시 계산해 Repository에 전달한다.
