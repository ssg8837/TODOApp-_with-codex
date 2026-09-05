# 프로젝트 문서

이 디렉터리는 ToDOApplication의 요구사항, 개발 규칙, 상세 설계를 관리한다.

## 문서 구분

- `requirements/`: 무엇을 만들어야 하는지 정의한다.
- `rules/`: 구현 전반에 적용되는 제약과 코딩 규칙을 정의한다.
- `design/`: 요구사항을 어떤 구조와 계약으로 구현할지 정의한다.

## 권장 읽기 순서

1. 제품 요구사항
2. 기능 및 비기능 요구사항
3. 아키텍처와 코딩 규칙
4. 패키지, 화면, 데이터 및 Presenter 상세 설계

## 문서 상태 표기

- `확정`: 구현 기준으로 사용할 수 있다.
- `초안`: 검토가 필요하지만 구현 방향의 기본값으로 사용할 수 있다.
- `미정`: 사용자의 결정 전에는 임의로 구현하지 않는다.

## 입력 문서와 적용 문서

`input/PRD.md`, `input/TRD.md`, `input/Requirements.md`는 요건정의 작업의 원본 자료다. 실제 구현 기준은 원본을 현재 구조에 맞게 정리한 다음 문서다.

- PRD → `requirements/product-requirements.md`
- Requirements → `requirements/functional-requirements.md`
- TRD의 품질 조건 → `requirements/non-functional-requirements.md`
- TRD의 구현 규칙 → `rules/`
- TRD의 상세 구조 → `design/`

원본은 추적을 위해 보존한다. 원본과 적용 문서가 충돌하면 임의로 선택하지 말고 보고하되, 사용자가 별도로 승인한 최신 적용 문서를 구현 기준으로 사용한다.

## 아키텍처 해석

사용자가 지정한 논리 구조는 MVP다. TRD의 ViewModel은 별도 MVVM 구조가 아니라 Presenter의 생명주기 및 상태 보존을 위한 AndroidX 구현 기반으로 해석한다. 자세한 기준은 `rules/architecture-rules.md`에 있다.
