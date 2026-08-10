# EV-006 · P1.6 parity inventory와 differential runner 기반

## 문제와 한계

기존 route를 화면·기능을 옮길 때마다 기억으로 추적하면 49개 page와 55개 API 중 일부가 조용히 빠진다. 또한 legacy와 Spring의 응답에는 UUID, timestamp, signed URL, trace ID처럼 의미와 무관한 실행별 값이 있어 raw JSON 비교만으로는 false failure가 발생한다.

## 선택과 구현

기준 repository의 `page.tsx`와 `api/**/route.ts`를 스캔해 49 page·55 route file inventory를 `docs/parity/matrix.yaml`에 고정했다. 각 API의 export method도 함께 기록하고, guest/member/staff logical fixture를 `migration/fixtures/logical-fixture.yaml`에 둔다. Java `ParityInventoryTest`는 count·중복·path template·method 누락을 build에서 차단한다. frontend `normalizePayload`는 volatile 값을 canonical token으로 치환한 뒤 legacy/Spring 의미 payload를 비교한다.

## 검증 결과

`./gradlew parityInventoryTest`에서 2개 inventory test가 통과했고, frontend Vitest는 normalization과 shell을 포함해 3개 테스트가 통과했다. `spring: pending` 상태는 아직 parity 완료를 주장하지 않는 명시적 표식이다.

## 다음에 측정할 것

P1.7에서 실제 dual-target HTTP runner를 fixture actor별로 연결하고, P2 vertical slice가 완료될 때마다 matrix의 `spring` 상태와 response/status/auth 증거를 갱신한다.
