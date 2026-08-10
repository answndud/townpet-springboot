# TownPet parity matrix

이 파일은 기준 TownPet의 49개 page와 55개 API route를 Spring 전환의 추적 단위로 고정한다. `docs/parity/matrix.yaml`이 기계가 읽는 inventory이고, 각 행의 `spring` 상태가 `pending → adapter → spring-owned → verified`로 진행된다. 현재 P1.6에서는 legacy inventory와 정규화 규칙만 확정했으며 기능 parity를 완료한 것으로 간주하지 않는다.

## 범위와 상태

| 대상 | 수량 | source | 현재 상태 | 다음 증거 |
|---|---:|---|---|---|
| page | 49 | `townpet/app/src/app/**/page.tsx` | legacy inventory captured | React route + screenshot |
| API route file | 55 | `townpet/app/src/app/api/**/route.ts` | legacy inventory captured | OpenAPI operation + differential response |
| HTTP method | route별 상이 | route export function | inventory에 기록 | status/error/auth test |

## Matrix contract

- page는 URL template, actor, fixture, responsive viewport, metadata, accessibility test ID를 갖는다.
- API는 path template, HTTP method, auth actor, request fixture, status/error, owner module과 test ID를 갖는다.
- UUID·timestamp·signed URL·trace ID처럼 실행마다 달라지는 값은 의미 비교 전에 normalize한다.
- `legacy: true`는 기준 구현이 존재한다는 뜻이고, `spring: pending`은 아직 Spring parity를 주장하지 않는다는 뜻이다.

## 자동 검증

```text
./gradlew parityInventoryTest
corepack pnpm -C frontend test -- parity
```

`ParityInventoryTest`는 YAML count·중복·경로 형식을 검증한다. `frontend/src/parity/normalize.ts`의 테스트는 두 target payload가 volatile 값만 다를 때 동일하다고 판단하는지 검증한다.
