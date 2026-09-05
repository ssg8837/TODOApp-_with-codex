# Jetpack Compose 규칙

상태: 확정

- Composable은 전달받은 상태를 렌더링하고 이벤트 콜백을 호출하는 역할에 집중한다.
- 화면 수준 Composable과 재사용 가능한 표시용 Composable을 분리한다.
- 상태는 가능한 한 상위로 끌어올리고 단일 상태 원천을 유지한다.
- Presenter의 `StateFlow`는 생명주기를 고려한 방식으로 수집한다.
- Composable 본문에서 파일, 데이터베이스 또는 네트워크 작업을 직접 실행하지 않는다.
- 부수 효과는 `LaunchedEffect` 등 목적에 맞는 Effect API로 제한한다.
- 불안정한 객체를 불필요하게 생성하여 재구성을 유발하지 않는다.
- 사용자 노출 문자열은 문자열 리소스를 사용한다.
- 색상과 타이포그래피는 `MaterialTheme`를 우선 사용한다.
- 주요 화면은 로딩, 빈 상태, 정상 상태와 오류 상태를 명시적으로 표현한다.
- 의미 있는 UI에는 Preview 또는 UI 테스트 가능성을 고려한다.
- Category 색상은 이름과 함께 표시하며 색상만으로 의미를 전달하지 않는다.
- Category 색상 선택은 작은 원형 버튼과 사전 정의 팔레트로 제공하고 선택 상태에 접근성 설명을 제공한다.
- Category drag & drop은 사용자 Category에만 허용하며 시스템 Category `일반`에는 이동 affordance를 제공하지 않는다.
