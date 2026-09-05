# 알림 및 알람 설계

상태: 확정

## 처리 흐름

```text
Todo/Reminder 저장
  -> 알림 시각 계산
  -> 권한 및 미래 시각 확인
  -> AlarmScheduler
  -> AlarmManager
  -> TodoAlarmReceiver
  -> NotificationManager
```

- 알림 시각은 `TODO 예정일시 - minutesBefore`다.
- `minutesBefore`의 MVP 값은 `15`, `1440`이며 복수 선택을 허용한다.
- 각 Alarm은 Todo와 Reminder를 함께 식별하는 안정적인 ID를 가진다.
- 날짜, 시간 또는 Reminder 변경 시 기존 Alarm을 취소하고 등록 가능한 Alarm을 다시 계산한다.
- TODO 삭제 시 미래 Alarm을 취소하고 Reminder도 삭제한다.

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

## 재부팅

`BOOT_COMPLETED` 수신 후 Room을 조회하여 현재보다 미래이고 미완료 TODO에 속한 Reminder만 다시 등록한다. 동일한 Alarm ID 규칙을 사용해 중복을 방지한다. 필요한 Manifest 권한과 Receiver만 선언한다.
