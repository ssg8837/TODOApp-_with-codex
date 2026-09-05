# 화면 상세 설계

상태: 초안

## TodoList

- 시작 화면이며 최초 선택 날짜는 오늘이다.
- 이전·다음 날짜 버튼을 제공하고 현재 표시 날짜를 선택하면 DatePicker로 이동할 날짜를 고른다.
- 해당 날짜 목록과 완료 변경을 제공한다.
- Category 선택과 미완료 필터를 AND 조건으로 적용한다.
- Category 필터는 `일반`을 최상단에 두고 저장된 사용자 Category 순서를 사용한다.
- TODO 추가, 수정 및 종류 관리 화면으로 이동한다.
- 날짜에 데이터가 없을 때와 필터 결과가 없을 때를 구분한다.
- 시간 있는 TODO를 시간 오름차순으로 먼저 표시하고, 무시간 TODO를 뒤에 표시한다. 같은 시간은 생성순 오름차순이며 완료 여부는 순서에 영향을 주지 않는다.

예상 상태: `selectedDate`, `todos`, `categories`, `selectedCategoryId`, `incompleteOnly`, `isLoading`, `errorMessage`.

## TodoEdit

- 등록과 수정이 같은 입력 화면을 공유할 수 있다.
- 제목, 날짜, 선택 시간, Category와 두 Reminder 옵션을 제공한다. Category 미선택 시 `일반`을 사용한다.
- Category 선택 목록은 저장된 순서를 사용하며 색상 표시와 이름을 함께 제공한다.
- 수정 모드는 Todo ID만 전달받아 Repository에서 다시 조회한다.
- 저장 중 중복 입력을 막고 실패하면 편집 내용을 유지한다.
- TODO 삭제는 AlertDialog에서 명시적으로 확인한 후 실행한다.
- 알림 권한은 사용자가 처음 Reminder를 활성화하려는 시점에 요청한다.

## CategoryManagement

- 시스템 기본 `일반`과 사용자 Category 목록, 사용자 Category 추가·이름 수정·삭제를 제공한다.
- `일반`에는 이름 수정과 삭제 동작을 제공하지 않는다.
- 사용자 Category 삭제는 AlertDialog에서 확인하며, 사용 중인 경우 Todo는 삭제되지 않고 Category가 `일반`로 변경된다고 안내한다.
- `일반`은 항상 최상단에 고정하고 drag handle을 제공하지 않는다.
- 사용자 Category에는 drag handle을 제공하고 사용자 Category끼리만 drag & drop으로 재정렬한다.
- 생성·수정 화면의 작은 원형 색상 버튼을 선택하면 8개의 사전 정의 색상 팔레트를 표시한다.
- 자유 RGB/HEX 입력은 제공하지 않으며 원형 색상과 Category 이름을 함께 표시한다.

## Navigation

목적지는 `TodoList`, `TodoEdit`, `CategoryManagement`다. 화면 간에는 전체 객체보다 ID와 필요한 최소 인자만 전달한다.

현재 `MainActivity`의 `Greeting`은 초기 템플릿이며 제품 화면이 아니다.
