# EV-010 — P2.1b demo identity와 deny-by-default role

- 시점·범위: P2.1b 인증 hardening, V003 migration
- 출처: `planned-upfront` + `implementation-discovery`

## Trigger

공개 showcase는 실제 회원가입·OAuth를 열지 않고 고정된 합성 계정으로 인증과 권한을 시연해야 한다. P2.1a의 모든 credential이 `MEMBER`로 고정되어 있어 Moderator 경계와 운영 URL의 deny-by-default를 증명할 수 없었다.

## 결정

role을 회원 aggregate에 억지로 연결하지 않고 identity credential의 인증 authority로 보관한다. 현재 showcase에는 MEMBER와 MODERATOR만 허용하고, ADMIN·OPERATOR credential은 만들지 않는다. `/api/v1/operations/**`는 MODERATOR role을 요구하며 나머지는 인증 여부와 별개로 명시된 matcher가 없으면 접근할 수 없다.

## 구현

- Flyway `V003__demo_identity_roles.sql`이 role check constraint를 추가하고 합성 MEMBER 3개와 MODERATOR 1개를 seed한다.
- demo password는 migration에 평문이 아닌 BCrypt hash로만 저장한다.
- `MemberUserDetailsService`가 credential role을 Spring authority로 변환한다.
- `IdentityMemberControllerTest`가 일반 member의 moderator operations 접근을 403으로 검증한다.
- `DatabaseBaselineTest`가 V003 role column과 moderator seed count를 Testcontainers PostgreSQL에서 확인한다.

## Trade-off와 남은 범위

credential role은 단순하고 identity module의 소유권을 지키지만, 향후 조직·resource별 staff 권한이 생기면 별도 RBAC/ABAC model과 audit가 필요하다. demo 계정의 scoped reset과 password lifecycle 잠금, OAuth provider stub은 아직 다음 slice다.

## 면접 답변

### 30초

공개 showcase에서 실제 가입을 열지 않기 위해 합성 demo identity를 Flyway로 결정적으로 seed했다. 모든 URL을 role로 장식하지 않고 운영 prefix만 MODERATOR로 제한하고, 기본 matcher는 deny-by-default로 두었다. BCrypt hash와 PostgreSQL migration test, 403 authorization test로 평문 credential과 권한 상승을 검증했다.

### 꼬리 질문

왜 role을 member 테이블에 두지 않았나? 인증 authority는 identity가 소유하고 member module 내부 entity를 identity가 참조하면 Modulith 경계가 뒤집히기 때문이다. 왜 ADMIN을 seed하지 않았나? 공개 포트폴리오에 필요하지 않은 고위험 권한을 노출하지 않기 위해서다.
