# 테스트 전략 감사 - 관찰 기록

## 실행 정보

- commit: `bc09b4a`
- 방법: 기존 34개 백엔드·37개 프론트엔드 테스트를 불변식·권한·여정 관점에서 매핑하고, 공백을 실제 회귀 위험 순으로 평가했다.

## 기존 커버리지 강점

| 영역 | 검증 수단 | 평가 |
|---|---|---|
| 동시성 | `RelationshipControllerTest` — CountDownLatch 기반 follow 경합, unique 유지 | ✓ 우수 |
| 권한·소유권 | 13개 `isForbidden` assertion + 세션 쿠키 기반 실제 로그인 플로우 | ✓ |
| DB 제약 | Testcontainers PostgreSQL + 실제 migration 실행 (12개 controller test) | ✓ |
| 오류 계약 | `GlobalProblemHttpTest` — RFC 9457 code/traceId 검증 | ✓ |
| E2E 실패 여정 | `domain-error-journeys.spec.ts` — 폼 보존, 401 리다이렉트, 422/409 UX | ✓ |
| 아키텍처 경계 | ArchUnit ModularityTest, LayerRulesTest | ✓ |

## 식별된 공백과 적용한 회귀 테스트

### GatheringControllerTest (신규 추가)

가장 큰 공백: gathering 모듈은 row lock(`findForUpdate`)으로 정원 경합을 방지하는 핵심 동시성 로직을 갖지만 **테스트가 전혀 없었다**. lock이 제거되거나 조건이 변경돼도 CI가 잡지 못한다.

| 테스트 | 보호하는 실제 장애 |
|---|---|
| `anonymousJoinIsRejected` | 인증 없는 참가 — @MemberOnly 누락 시 발생 |
| `duplicateJoinIsIdempotent` | 중복 참가로 participant row 중복 생성 — unique constraint 회귀 감지 |
| `concurrentJoinsNeverExceedCapacityOrDuplicateParticipant` | 6명 동시 요청에 정원 3 초과 — findForUpdate 제거·조건 변경 시 즉시 실패. lost update 방지 |
| `cancelledGatheringRejectsNewJoins` | 취소된 모임 참가 허용 — 상태 검사 누락 감지 |

## 남은 미검증 리스크 (우선순위)

| 리스크 | 현재 상태 | 권장 접근 |
|---|---|---|
| Reaction count 정확성 | unique constraint 있으나 count 집계 정합성 미검증 | reaction toggle 후 count 단일 통합 테스트 |
| Media 업로드 크기·형식 거부 | MaxUploadSize 핸들러만 존재, 실제 초과 업로드 테스트 없음 | MockMvc multipart 경계 1건 |
| LostFound 위치 암호화 | 서비스 구현 확인됨, 암호화 roundtrip·공개 응답 마스킹 테스트 없음 | 민감정보 노출 회귀 1건 |
| 이벤트 publication 재시도 idempotency | P3에서 DB constraint로 해결했으나 재시도 시나리오 자체 미실행 | Modulith integration test 1건 |
| CSRF 만료·갱신 흐름 | e2e에서 부분적 커버 | 낮은 우선순위 — 운영 관찰 |

## 원칙 준수 확인

추가한 테스트는 구현 디테일이 아닌 HTTP 응답 계약만 검증하고, sleep 없이 latch로 동기화하며, 임의 네트워크·시간 의존이 없다. 같은 동작을 unit/integration/e2e로 중복 검증하지 않았다.
