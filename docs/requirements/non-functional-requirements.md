# 비기능 요구사항

상태: 확정  
근거: `docs/input/TRD.md`

## 기술 및 호환성

- Android 네이티브 앱으로 Kotlin과 Jetpack Compose를 사용한다.
- 프로젝트 Gradle Wrapper를 사용하며 전역 Gradle 설치를 요구하지 않는다.
- 최소 지원 버전은 현재 프로젝트의 `minSdk 24`를 기준으로 한다.
- 라이브러리 버전은 Version Catalog 또는 Gradle 설정 한 곳에서 관리한다.

## 구조와 유지보수성

- Compose View, Presenter, Application Service, Repository, DAO/Room 및 시스템 API 책임을 분리한다.
- 비즈니스 로직은 Android UI 없이 단위 테스트할 수 있어야 한다.
- 로컬 우선 구조를 사용하며 Room을 데이터의 Source of Truth로 삼는다.
- 요구사항 변경에 대응할 수 있어야 하지만 불필요한 프레임워크와 추상화는 추가하지 않는다.
- Hilt/Koin은 사용하지 않고 constructor injection을 사용한다. Application 수준 `AppContainer`는 Repository와 Application Service를 조립하고 필요한 ViewModel Factory는 Service를 Presenter에 주입한다.
- Room Entity와 Domain Model을 분리하고 mapper로 변환한다. Network DTO는 만들지 않는다.

## 성능과 비동기 처리

- DB와 시스템 작업으로 UI 스레드를 장시간 차단하지 않는다.
- 비동기 작업에는 Kotlin Coroutines를 사용한다.
- 즉시 완료되는 로컬 목록 조회에 불필요한 전체 화면 Spinner를 표시하지 않는다.
- 저장처럼 중복 실행이 문제를 만드는 동작은 중복 입력을 방지한다.

## 신뢰성과 오류

- 앱 종료 후에도 데이터가 유지되어야 한다.
- 알람은 DB의 Reminder를 사용해 재구성 가능해야 한다.
- 권한 거부, DB 실패와 알람 실패가 앱 비정상 종료로 이어지지 않아야 한다.
- 내부 예외는 사용자용 메시지와 개발 진단 정보로 분리한다.

## 접근성 및 사용성

- 아이콘 버튼에는 의미 있는 접근성 설명을 제공한다.
- 시스템 글꼴 크기, 다크 테마와 화면 크기 변화를 고려한다.
- 색상만으로 상태를 구분하지 않는다.
- 로딩, 빈 상태, 정상 상태와 오류 상태를 명시적으로 표현한다.

## 보안과 권한

- 비밀키와 민감한 데이터를 소스나 로그에 노출하지 않는다.
- 실제 사용하는 기능에 필요한 최소 권한만 Manifest에 선언한다.
- Android 13 이상 알림 권한 및 정확한 알람 관련 시스템 정책을 준수한다.
- 시스템 정책이나 권한을 우회하지 않는다.
- 알림 권한은 최초 실행이 아니라 사용자가 처음 Reminder를 활성화하려는 시점에 요청한다.

## 데이터 보존

- Room 초기 version은 `1`로 한다.
- schema version을 올릴 때 명시적인 Migration을 작성한다.
- `fallbackToDestructiveMigration` 계열을 기본 정책으로 사용하지 않는다.
- 개발 중 초기화가 필요하면 앱 데이터를 명시적으로 삭제한다.
