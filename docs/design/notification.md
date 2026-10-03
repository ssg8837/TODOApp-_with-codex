# 알림 및 알람 설계

상태: 확정

## 처리 흐름

```text
Todo/Reminder 저장
  -> TodoService 또는 ReminderService
  -> 알림 시각 계산
  -> 권한 및 미래 시각 확인
  -> AlarmScheduler
  -> AlarmManager
  -> TodoAlarmReceiver
  -> NotificationManager
```

- `AlarmScheduler`는 Repository 내부에 숨기지 않는 별도 외부 시스템 abstraction이다.
- `TodoService` 또는 `ReminderService`가 TodoRepository, ReminderRepository와 AlarmScheduler를 조합한다.
- Service는 저장과 Alarm 실행 순서를 제어하지만 DB transaction의 내부 절차는 Repository에 위임한다.

- 알림 시각은 `TODO 예정일시 - minutesBefore`다.
- `minutesBefore`의 MVP 값은 `15`, `1440`이며 복수 선택을 허용한다.
- 각 Alarm은 Todo와 Reminder를 함께 식별하는 안정적인 ID를 가진다.
- 날짜, 시간 또는 Reminder 변경 시 기존 Alarm을 취소하고 등록 가능한 Alarm을 다시 계산한다.
- TODO 삭제 시 미래 Alarm을 취소하고 Reminder도 삭제한다.
- TODO 편집 저장은 Todo 영속화 후 실제 Todo ID로 `ReminderRepository.replaceReminders()`를 호출한다. Reminder의 삭제·일괄 삽입은 하나의 Room transaction이지만 현재 Repository 경계상 Todo 변경과 Reminder 교체는 서로 다른 transaction이다.
- DB 저장 뒤 Alarm 동기화가 실패해도 DB를 롤백하지 않으며 `TodoService.resynchronizeAlarms(todoId)`로 저장 상태를 다시 적용할 수 있다.
- 예약 PendingIntent는 고정 request code와 `todo-reminder://alarm/{todoId}/{reminderId}` data URI 조합으로 식별한다. `Long` ID를 request code로 축소하지 않는다.

## 과거 Reminder

- 과거 날짜와 과거 시간의 TODO 저장을 허용한다.
- 계산된 Reminder 시각이 현재보다 과거여도 Reminder 데이터는 저장한다.
- 이미 지난 Reminder에는 시스템 Alarm을 등록하지 않는다.
- 재부팅 중 지나간 Reminder도 알림을 발생시키지 않는다.

## 완료 상태 변경

- TODO 완료 시 아직 실행되지 않은 모든 미래 Alarm을 취소한다.
- Reminder 데이터는 삭제하지 않는다.
- 완료된 TODO를 미완료로 되돌리면 현재보다 미래인 Reminder만 다시 등록한다.

## Exact와 Inexact Alarm

- Exact Alarm은 MVP의 필수 성공조건이 아니다.
- Exact Alarm 사용 가능 여부를 확인하고 가능하면 Exact Alarm을 사용한다.
- Exact Alarm 권한 또는 특수 접근이 없으면 Android가 허용하는 Inexact Alarm으로 fallback한다.
- 시스템 정책이나 권한을 우회하지 않는다.
- Exact API 호출 직전에 권한이 바뀌어 `SecurityException`이 발생하면 Inexact 방식으로 재시도한다.

## 알림 표시 권한

- 앱 최초 실행 시 알림 권한을 요청하지 않는다.
- 사용자가 처음 Reminder 기능을 활성화하려는 시점에 요청한다.
- 권한이 없으면 Todo와 Reminder는 저장하지만 시스템 Alarm은 등록하지 않는다.
- 사용자에게 알림을 사용할 수 없음을 안내하며 앱이 종료되어서는 안 된다.

## 예약 실패

- Alarm 등록 실패로 Todo와 Reminder 저장을 롤백하지 않는다.
- UI에 `일정은 저장되었지만 알림을 예약하지 못했습니다.`에 해당하는 사용자 안내 상태를 전달한다.
- 실패 후 UI가 무한 로딩 상태에 머물지 않는다.

## Notification Channel

앱의 알림 기능 초기화 시 TODO 알림용 Notification Channel 하나를 생성한다.

Receiver는 Intent의 Reminder ID와 예약 시각을 키로 사용하되, 알림 표시 직전에 Room을 다시 조회한다. Todo/Reminder 삭제, 완료, 예정 시각 변경이 확인되면 늦게 도착한 broadcast를 무시한다. 알림 선택 시 Todo ID만 전달하여 수정 화면에서 최신 데이터를 다시 읽는다.

## 재부팅

`BOOT_COMPLETED` 수신 후 Room을 조회하여 현재보다 미래이고 미완료 TODO에 속한 Reminder만 다시 등록한다. 동일한 Alarm ID 규칙을 사용해 중복을 방지한다. 필요한 Manifest 권한과 Receiver만 선언한다.

- `BootReceiver`는 `goAsync()`로 수신 수명을 연장하고 `ReminderRecoveryService` 호출만 담당한다. DAO, Repository, AlarmManager와 복구 판정 로직에 직접 접근하지 않는다.
- `ReminderRecoveryService`는 Room 기반 후보마다 최신 Reminder와 Todo를 다시 조회하고, 삭제·변경·완료·시간 없음·현재 시각 이하 발화 시각을 제외한다.
- 발화 시각은 Phase 11의 `ReminderCalculator`와 주입된 `Clock`/`ZoneId`를 재사용한다.
- 유효 Reminder는 기존 `AlarmScheduler.schedule()`로 재예약하므로 PendingIntent identity와 Exact→Inexact fallback 정책이 동일하다.
- 후보 하나의 조회 또는 예약 실패는 다른 후보의 복구를 중단하지 않는다. 결과는 전체 성공, 일부 실패, 전체 실패와 건수로 구분하되 부팅 시 UI를 표시하지 않는다.
- 복구 트리거는 `BOOT_COMPLETED`만 사용하며 앱 시작, 시간대·시각 변경, 앱 업데이트에는 전체 DB 스캔을 추가하지 않는다.
