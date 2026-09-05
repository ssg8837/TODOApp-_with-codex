# Project Instructions

## 문서 읽기 순서

코드를 작성하거나 변경하기 전에 다음 문서를 순서대로 읽는다.

1. `docs/README.md`
2. `docs/requirements/product-requirements.md`
3. `docs/requirements/functional-requirements.md`
4. `docs/requirements/non-functional-requirements.md`
5. `docs/rules/architecture-rules.md`
6. `docs/rules/kotlin-style-guide.md`
7. `docs/rules/compose-rules.md`
8. `docs/rules/testing-rules.md`
9. `docs/design/architecture.md`
10. 변경 대상과 관련된 `docs/design/` 문서

## 기본 원칙

- Kotlin과 Jetpack Compose를 사용한다.
- 애플리케이션 구조는 이 프로젝트에서 정의한 MVP 규칙을 따른다.
- TRD의 ViewModel은 Presenter 역할의 AndroidX 생명주기 구현 기반으로 해석한다.
- 요구사항과 설계 문서에 없는 제품 동작을 임의로 확정하지 않는다.
- 문서끼리 충돌하거나 문서와 기존 구현이 충돌하면 구현 전에 충돌 내용을 보고한다.
- 요청 범위를 벗어난 기능 추가와 리팩터링을 하지 않는다.
- 기존 사용자 변경 사항을 보존한다.
- 기능 변경에는 그 위험도에 맞는 테스트를 추가하거나 갱신한다.
- 구현이 문서화된 계약이나 구조를 변경하면 관련 문서도 함께 갱신한다.
- `docs/input/`은 원본 자료이며 실제 구현 기준은 구조화된 `requirements/`, `rules/`, `design/` 문서다.

## 우선순위

지침이 충돌하면 다음 우선순위를 적용한다.

1. 사용자가 현재 요청에서 명시한 내용
2. `docs/requirements/`의 승인된 요구사항
3. `docs/design/`의 상세 설계
4. `docs/rules/`의 공통 규칙
5. 기존 코드의 관례
