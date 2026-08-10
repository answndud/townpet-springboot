# Demo identity와 deny-by-default RBAC

## 무엇을 해결하는가

공개 showcase에서 실제 개인정보를 받지 않고도 login·권한 경계를 재현한다. credential에는 평문 password가 아니라 adaptive BCrypt hash와 최소 role만 둔다.

## 적용 위치

- `V003__demo_identity_roles.sql`: MEMBER 3개와 MODERATOR 1개 합성 identity
- `CredentialEntity.role`, `MemberUserDetailsService`: authority 변환
- `SecurityConfig`: `/api/v1/operations/**`의 `ROLE_MODERATOR` gate
- `DatabaseBaselineTest`, `IdentityMemberControllerTest`: seed·403 evidence

## Failure mode

모든 authenticated 사용자를 운영 API에 허용하면 privilege escalation이 된다. role 문자열을 임의로 받거나 평문 seed를 commit하면 credential leakage가 된다. 그래서 DB check constraint, 고정 migration, hash-only seed, endpoint authorization test를 함께 둔다. 실제 운영 staff와 resource attribute가 필요해질 때는 별도 ADR로 RBAC/ABAC와 audit를 확장한다.

## 면접 체크

- demo 계정은 실제 회원가입을 대신하지 않는다: showcase용 합성 fixture다.
- 401은 인증 실패, 403은 인증은 됐지만 role 정책을 통과하지 못한 경우다.
- deny-by-default는 matcher에 등록되지 않은 보호 API를 실수로 공개하지 않는 기본선이다.
