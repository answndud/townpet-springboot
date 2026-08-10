# EV-004 · P1.4 OpenAPI contract-first transport

## 문제와 한계

Spring controller DTO와 React 타입을 각각 작성하면 필드명·상태 코드·오류 형식이 조금씩 달라지는 contract drift가 생긴다. 기존 TownPet에는 여러 route handler가 각자 입력·응답을 정의하고 있어, 프런트엔드 이관과 backend 전환을 병행할수록 drift를 늦게 발견할 위험이 컸다.

## 선택과 구현

OpenAPI 3.1을 `/api/v1` HTTP contract의 단일 source로 두고, OpenAPI Generator에서 Spring Java transport interface와 TypeScript fetch client를 각각 생성한다. 생성 영역은 `build/generated`에만 두어 domain aggregate·JPA entity·repository가 transport에 침투하지 않게 했다. RFC 9457 ProblemDetail 공통 필드(`code`, `traceId`, `fieldErrors`)는 `GlobalProblemHandler`로 고정했다.

## 검증 결과

`openApiValidate`가 `api/openapi/townpet.yaml`을 유효한 OpenAPI 3.1 문서로 판정했고, `generateOpenApiClients`가 Java·TypeScript source를 생성했다. `contractTest`는 대표 route, UUID·UTC·idempotency·ProblemDetail 규칙과 양쪽 generated output 존재를 확인한다. 전체 `clean check`에서는 이 contract gate를 함께 실행한다.

## 다음에 측정할 것

vertical slice가 추가될 때 각 controller가 생성 Java interface를 구현하도록 연결하고, additive/breaking 변경을 분류하는 diff gate와 실제 MockMvc response contract를 추가한다.
