# OpenAPI contract-first와 ProblemDetail 면접 노트

## 핵심 개념

OpenAPI는 endpoint path만 나열하는 문서가 아니라 request, response, status code, security, pagination과 error를 함께 정의하는 실행 가능한 경계다. Java·TypeScript 타입을 같은 문서에서 생성하면 언어별 수작업 DTO 복제를 줄이고, transport와 domain 모델을 분리할 수 있다.

RFC 9457 ProblemDetail은 오류의 HTTP 표현을 표준화한다. TownPet은 표준 `type/title/status/detail/instance`에 안정적인 `code`, 추적용 `traceId`, 입력 오류의 `fieldErrors`를 확장 속성으로 둔다.

## 도입 순서와 trade-off

처음부터 55개 API를 모두 생성하지 않고, 인증·feed·publication 대표 route와 공통 규칙으로 계약 파이프라인을 먼저 증명했다. OpenAPI Generator가 3.1 지원을 아직 beta로 표시하므로 generated output을 직접 편집하지 않고 validation·재생성 gate를 둔다. domain model까지 생성하면 persistence 결합이 커지므로 transport interface·DTO 범위만 생성한다.

## 면접 답변 포인트

- API 계약의 source of truth가 Java controller가 아니라 OpenAPI 문서다.
- `Idempotency-Key`, cursor, `If-Match` 같은 재시도·동시성 의미를 문서에 먼저 남긴다.
- 400/401/404/409를 ProblemDetail로 통일해 client가 임의 문자열을 파싱하지 않게 한다.
- 생성 코드는 경계에만 두고 application command·aggregate·JPA entity는 명시적 변환으로 보호한다.
