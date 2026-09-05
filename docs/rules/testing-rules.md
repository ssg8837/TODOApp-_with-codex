# 테스트 규칙

상태: 확정

## Repository 테스트

- Entity↔Domain 변환, DAO 호출과 데이터 저장·조회를 검증한다.
- Category 삭제·재정렬 등 기존 DB transaction 사용을 검증한다.
- Room/SQLite 예외가 내부 구현 그대로 노출되지 않고 데이터 계층 오류로 변환되는지 검증한다.

## Application Service 테스트

- 비즈니스 규칙과 `TodoValidator`, `CategoryValidator`, `ReminderValidator` 적용을 검증한다.
- 여러 Repository 호출 및 데이터 변경 순서와 Todo/Reminder 유스케이스 조합을 검증한다.
- 유효하지 않은 상태에서는 Repository 변경이 호출되지 않는지 검증한다.
- 시스템 Category 보호와 Category 순서 정책을 검증한다.
- 가능한 경우 Fake Repository를 사용한 순수 JVM Unit Test로 작성한다.

## Presenter 테스트

- 사용자 이벤트, Application Service 호출과 `UiState` 전이를 검증한다.
- 성공, 빈 데이터, Service 검증 실패, 비즈니스 오류와 중복 요청을 포함한다.
- Service 결과가 사용자 표시 오류로 변환되는지 검증한다.
- 실제 Room이나 Repository 구현을 사용하지 않고 Fake Service를 사용한다.
- 시간, dispatcher와 시스템 스케줄러는 테스트 가능한 경계로 분리한다.

## DAO 테스트

- Todo, Category와 Reminder의 등록·조회·수정·삭제
- 날짜, 종류, 완료 여부 및 복합 필터
- Todo 삭제 시 Reminder 관계 삭제
- 최초 DB 생성 시 시스템 기본 Category `일반` 생성
- Category 이름 unique 제약 및 `일반` 수정·삭제 차단
- 사용자 Category 삭제 시 연결 Todo를 `일반`로 재지정한 뒤 삭제하는 transaction
- `일반`의 `sortOrder = 0`, `color = NEUTRAL` 유지
- 신규 사용자 Category의 마지막 순서 추가와 재정렬 후 `sortOrder` 일관성
- Category 순서 변경 transaction 및 앱 재실행 후 순서 유지
- `CategoryColor`의 안정적인 Room 저장·복원
- 시간이 있는 Todo 우선, 시간 및 생성순 오름차순 정렬

## Compose UI 테스트

- TODO 등록, 완료 변경, 날짜 이동과 필터 적용의 핵심 흐름을 검증한다.
- 로딩, 빈 상태, 오류 안내와 입력값 유지 여부를 검증한다.
- 사용자에게 보이는 텍스트 또는 안정적인 semantics로 요소를 찾는다.
- `일반` 고정, 사용자 Category drag & drop, 색상 팔레트 선택을 검증한다.
- Category 선택과 필터 목록이 저장된 순서를 사용하고 색상과 이름을 함께 표시하는지 검증한다.

## 알림 테스트

- 알람 등록·취소와 TODO 수정 후 재등록
- 알림 권한 및 정확한 알람 사용 불가 상태
- 과거 Reminder 미등록, 완료 시 취소, 미완료 복귀 시 미래 알람 재등록
- Exact 사용 가능 시 Exact, 불가 시 Inexact fallback
- 알림 권한 없음과 예약 실패 시 저장 유지 및 안내
- 재부팅 시 미래 Reminder만 복구
- Service 테스트에서는 Fake Repository와 Fake AlarmScheduler로 저장·예약 순서 및 부분 실패 정책을 검증한다.

## 완료 기준

- 변경 로직과 직접 관련된 테스트가 통과해야 한다.
- 실행하지 못한 검증은 이유와 범위를 보고한다.
- 테스트 이름에는 조건, 동작과 기대 결과가 드러나야 한다.
