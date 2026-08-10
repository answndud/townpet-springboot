# Technical Notes

이 문서는 일반 기술 사전이 아니라 현재 TownPet 코드로 설명할 수 있는 개념만 요약한다. 세부 결정은 ADR/TRD, 진행 순서는 PLAN을 기준으로 한다.

## Java·Gradle·Spring 기반

- Gradle Wrapper는 build tool 버전을 고정하고 Java toolchain은 compile JDK를 선택한다. `options.release=25`는 생성 bytecode/API 수준을 맞춘다.
- `contextLoads`는 bean graph와 auto-configuration 검증이지 PostgreSQL 호환성이나 기능 완료 증거가 아니다.
- Spotless는 형식, Error Prone·NullAway는 compile-time 오류 패턴과 null 계약, JaCoCo는 실행된 test coverage를 담당한다. 서로 대체하지 않는다.
- 근거: `build.gradle.kts`, `TownPetApplicationTests`, `./gradlew clean check`

## PostgreSQL·Flyway

- Flyway만 versioned schema를 변경하고 Hibernate `ddl-auto=validate`는 mapping drift를 확인한다.
- PostGIS·citext extension은 bootstrap admin, migration과 runtime은 제한된 application role 책임이다.
- H2는 빠른 web/context test, Testcontainers PostgreSQL은 실제 dialect·constraint·migration test, Compose는 반복 가능한 local runtime에 사용한다.
- Modulith event publication과 Spring Session table도 Flyway가 소유해 자동 schema 초기화와 충돌하지 않게 한다.
- 실제 PostgreSQL 기동 검증에서 H2가 허용한 `CHAR`/`VARCHAR`와 `citext` mapping drift를 발견했다. 적용 migration은 유지하고 V006과 명시적 JPA column type으로 정렬했다.
- 근거: `deploy/compose`, Flyway V001~V006, `DatabaseBaselineTest`, Credentials browser E2E

## Modular monolith와 API 계약

- 모듈은 기술 layer가 아니라 변경 이유와 data write ownership으로 나눈다. 다른 모듈의 JPA entity/repository 대신 식별자, 공개 application API 또는 event를 사용한다.
- Publication은 작성자 UUID만 소유하고 Member의 공개 `MemberDirectory`로 현재 대표 동네를 확인한다. `LOCAL/GLOBAL`과 동네 필드의 구조적 일관성은 DB 제약으로, 실제 회원 동네 소유권은 transaction 안의 application policy로 검증한다.
- Publication write는 JPA aggregate로 처리하고 최신 피드는 jOOQ read path에서 `(createdAt, id)` keyset cursor로 조회한다. `GLOBAL` audience는 로그인 cookie가 있어도 동네 글을 섞지 않고, `VIEWER`만 현재 회원의 대표 동네를 추가한다.
- Publication 변경 명령의 actor는 session principal에서만 가져오며 `authorMemberId`와 일치해야 한다. Client가 읽은 `version`을 먼저 비교하고 JPA `@Version`이 검사 이후의 경합도 감지해 stale write를 `409`로 반환한다. 삭제는 row 제거가 아니라 `ACTIVE → DELETED` lifecycle 전이이며 상세와 피드가 같은 상태 조건으로 즉시 제외한다.
- Engagement의 일반 Comment는 `publicationId` 값만 저장하고 publication 모듈의 `PublicationAccess` 공개 API로 부모의 `ACTIVE` 여부만 확인한다. 댓글 작성자 ID는 인증 principal에서만 가져오며, 목록은 `createdAt + id` 오름차순으로 고정하고 삭제는 댓글 자체의 `ACTIVE → DELETED` 전이로 처리한다.
- Reaction은 삭제 lifecycle 대신 원장 row의 존재 여부로 `LIKE` 활성 상태를 표현한다. `(publicationId, authorMemberId, type)` unique constraint와 명시적인 `active` PUT을 함께 사용해 같은 요청을 반복해도 중복 row나 상태 반전이 생기지 않으며, count와 현재 회원 상태는 같은 transaction 경계에서 반환한다.
- Bookmark는 reaction count와 분리된 회원별 저장 원장이다. `(publicationId, memberId)` unique constraint와 명시적인 `active` PUT으로 상태를 멱등하게 바꾸고, 상세 GET은 비회원도 `active=false`를 읽을 수 있지만 변경은 session principal만 허용한다. 삭제된 publication은 `PublicationAccess`에서 차단하며, `V010`, `BookmarkControllerTest`, `bookmark-management.spec.ts`가 이 경계를 검증한다.
- Follow와 block은 서로 다른 원장과 unique 제약으로 분리한다. 한 번의 relationship PUT에서 block을 켜면 follow를 제거해 상충 상태를 없애고, 자기 자신은 DB check와 application policy 양쪽에서 차단한다. 상세 화면의 relationship 조회는 publication의 `authorId`를 통해서만 수행하며, `V011`, `RelationshipControllerTest`, `relationship-management.spec.ts`가 중복·IDOR·새로고침 상태를 검증한다.
- follow와 block은 서로 다른 원장이므로 unique constraint만으로 상호 배타성을 보장할 수 없다. 동일 viewer-target 쌍의 mutation 시작 시 PostgreSQL transaction advisory lock을 획득해 조회·삭제·삽입 구간을 직렬화하고, `RelationshipControllerTest.concurrentFollowAndBlockRequestsNeverLeaveBothRelationshipRows`로 병렬 요청에서도 한 원장만 남는지 검증한다.
- Relationship은 `BlockDirectory` 공개 API만 제공하고 publication/discovery가 `BlockEntity`나 repository를 직접 참조하지 않는다. `VIEWER` feed와 회원 상세만 차단 작성자를 제외하며 `GLOBAL` feed·비회원 상세는 공개 정책을 유지한다. 차단 작성자 상세는 회원에게도 `404`로 수렴해 feed와 direct URL의 정책 차이를 없앤다.
- Follow/block 활성화는 단순 `find → save`가 아니라 PostgreSQL `ON CONFLICT DO NOTHING` upsert를 사용한다. 애플리케이션 멱등성 검사와 DB unique constraint를 함께 두고, 실제 병렬 MockMvc 요청에서도 한 원장만 남도록 검증한다. 관계 조회는 항상 authenticated principal을 viewer로 사용해 다른 회원의 상태를 읽거나 바꿀 수 없다.
- Engagement는 `PublicationAccess.activeAuthorMemberId`와 `BlockDirectory`를 함께 사용해 publication 작성자 차단 정책을 재확인한다. 차단 회원의 댓글 목록·작성, reaction·bookmark 상태 조회·변경은 모두 `404`로 수렴하고, 비회원 공개 읽기는 유지해 UI별 정책 차이를 만들지 않는다.
- V012는 세 engagement 원장에 PostgreSQL `BEFORE INSERT` guard를 추가한다. 애플리케이션 정책 조회와 block 전환 사이에 경합이 생겨도 차단된 actor의 새 원장 삽입은 DB에서 거부되며, 서비스는 이를 publication-not-found 정책 오류로 변환한다. 캐시는 도입하지 않아 별도 무효화나 stale state가 없다.
- block 해제 뒤에는 같은 authenticated principal이 댓글·reaction·bookmark를 다시 생성할 수 있어야 한다. `BlockedEngagementPolicyTest.unblockingRestoresEngagementCreationForTheSameMember`는 해제 요청 후 세 API가 성공하고 각 source row가 하나씩만 생성되는지 확인해, 차단 중 거부 정책과 해제 후 복구 정책이 분리되지 않도록 한다.
- engagement 상태 검증은 HTTP 응답만 보지 않고 source row와 다시 읽은 요약을 함께 확인한다. 댓글 삭제는 row를 `DELETED`로 남기되 목록에서 제외하고, reaction·bookmark 비활성화는 원장을 제거해 count/active가 0 또는 false로 돌아가도록 `CommentControllerTest`, `ReactionControllerTest`, `BookmarkControllerTest`에서 검증한다.
- Spring Modulith는 module/cycle을, ArchUnit은 내부 package와 type 노출 규칙을 검사한다.
- OpenAPI는 HTTP transport의 source of truth다. Java·TypeScript transport 코드는 생성하지만 aggregate·entity·repository는 생성하지 않는다.
- ProblemDetail은 status와 기계 판독 code, traceId, field error를 한 오류 계약으로 묶는다.
- 근거: `MemberDirectory`, `BlockDirectory`, `PublicationService`, `PublicationFeed`, `PublicationAccess`, `CommentService`, `ReactionService`, `BookmarkService`, `RelationshipService`, `BlockedEngagementPolicyTest` (`blockedMemberCannotReadOrMutateEngagementButGuestCanRead`, `unblockingRestoresEngagementCreationForTheSameMember`), V007~V012, `PublicationControllerTest`, `CommentControllerTest`, `ReactionControllerTest`, `BookmarkControllerTest`, `RelationshipControllerTest`, `feed-parity.spec.ts`, `publication-management.spec.ts`, `comment-management.spec.ts`, `reaction-management.spec.ts`, `bookmark-management.spec.ts`, `relationship-management.spec.ts`, `ModularityTest`, `LayerRulesTest`, `api/openapi/townpet.yaml`, `OpenApiContractTest`

## React·Vite와 parity

- Vite는 정적 frontend build와 local `/api` proxy만 담당하고 production Node server 역할을 갖지 않는다.
- parity는 내부 구현 동일성이 아니라 같은 actor·state에서 사용자가 관찰하는 의미가 같은지 비교한다.
- UUID·시간·서명 URL 같은 volatile field만 allowlist로 normalize한다. status, permission, business field는 그대로 비교한다.
- backend/frontend 단위 gate와 실제 두 프로세스 smoke, browser E2E를 분리해 실패 위치와 실행 비용을 조절한다.
- 인증 browser E2E 스크립트는 기본적으로 `corepack pnpm`을 사용하되 `TOWNPET_PNPM_BIN`으로 로컬 pnpm 실행 파일을 주입할 수 있다. 이를 통해 Node/Corepack 버전 차이와 제품 기능 실패를 분리하면서도 같은 Docker PostgreSQL·Spring Boot·Vite 흐름을 검증한다.
- 근거: `frontend`, `docs/parity/matrix.yaml`, `ParityInventoryTest`, CI workflow, smoke script

## Session·CSRF·authorization

- 브라우저에는 opaque session identifier만 두고 SecurityContext는 Spring Session JDBC에 저장한다. 로그인 시 session을 먼저 만든 뒤 ID를 교체해 fixation을 방어한다.
- CSRF token은 `XSRF-TOKEN` cookie와 응답 body로 전달하고 React가 변경 요청의 `X-XSRF-TOKEN` header로 돌려준다.
- logout은 cookie UI만 바꾸는 것이 아니라 서버 session을 invalidate한다. 같은 session으로 보호 API를 다시 호출해 401을 확인한다.
- Credentials 로그인은 `email_verified_at`이 설정된 계정만 허용한다. 미인증·비활성·잘못된 비밀번호는 계정 상태를 노출하지 않도록 같은 `401` 응답으로 처리한다.
- Spring Session은 core library와 table만으로 활성화되지 않는다. Boot 4의 JDBC starter가 repository/filter auto-configuration을 제공하며, 테스트는 `SESSION` cookie와 repository row를 직접 확인한다.
- Password reset과 email verification token은 raw 값을 한 번만 전달하고 DB에는 SHA-256 hash, expiry와 optimistic version만 저장한다. Reset 성공은 credential·audit·token과 해당 principal의 JDBC session을 함께 변경하고, email verification 성공은 같은 이메일의 token을 모두 제거한다.
- Request service는 raw token을 반환하지 않고 `AccountTokenDelivery` 경계로 넘긴다. `local`·`test`·`e2e` profile만 메모리 capture를 사용하며 다른 환경에 adapter가 없으면 `503`과 transaction rollback으로 전달할 수 없는 token row를 남기지 않는다. E2E token 조회 API와 fixture는 `e2e` profile에서만 활성화된다.
- 현재 회원 ID는 request body/path가 아니라 authenticated principal에서 가져와 profile·pet IDOR 표면을 줄인다.
- 401은 인증 부재/실패, 403은 인증됐지만 role이 부족한 경우다. 운영 prefix는 MODERATOR만 허용한다.
- demo identity는 실제 사용자 데이터가 아닌 합성 fixture이며 password 평문은 저장하지 않는다.
- 근거: `SecurityConfig`, `SessionController`, `AccountTokenDelivery`, V002~V006, `IdentityMemberControllerTest`, `AccountTokenDeliveryUnavailableTest`, `auth-parity.spec.ts`

## 현재 학습·증거의 빈틈

- 실제 email provider adapter, transaction 이후 durable delivery와 retry·bounce 처리
- publication 복구와 media lifecycle
- event retry/idempotency, concurrency mutation, generated jOOQ schema와 feed projection
- 성능 수치, query plan, backup/restore와 배포 관측 evidence

이 항목들은 구현·실험 근거가 생길 때 해당 절에 추가한다. 미리 일반론을 채우지 않는다.
