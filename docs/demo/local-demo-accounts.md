# 로컬 데모 계정과 샘플 데이터

이 문서는 `townpet-springboot`의 로컬 Docker 포트폴리오 sandbox를 테스트하기 위한 합성 계정 안내다. 아래 계정과 비밀번호는 의도적으로 공개된 값이며 실제 서비스·실제 개인정보·공개 배포 환경에서 재사용하면 안 된다.

처음 실행하는 경우 [로컬 서버 실행 workflow](./local-development-workflow.md)를 먼저 따라 한다.

## 계정

| 구분 | 이메일 | 비밀번호 | 닉네임 | 역할 | 용도 |
| --- | --- | --- | --- | --- | --- |
| 일반 사용자 1 | `demo-member-1@townpet.local` | `townpet-demo-123!` | `demo-member-1` | `MEMBER` | 글 작성자, 댓글·좋아요·북마크 테스트 |
| 일반 사용자 2 | `demo-member-2@townpet.local` | `townpet-demo-123!` | `demo-member-2` | `MEMBER` | 다른 회원 관점의 댓글·지원·참여 테스트 |
| 일반 사용자 3 | `demo-member-3@townpet.local` | `townpet-demo-123!` | `demo-member-3` | `MEMBER` | 제3자 권한과 상호작용 테스트 |
| 운영 관리자 | `demo-moderator@townpet.local` | `townpet-moderator-123!` | `demo-moderator` | `MODERATOR` | `/admin` 운영 콘솔, 신고·운영 로그 테스트 |

현재 애플리케이션의 관리자 게이트는 `MODERATOR` 역할이다. 별도의 `ADMIN`·`OPERATOR` 권한은 만들지 않았으므로 위 운영 관리자 계정으로 관리자 화면을 확인한다.

운영 관리자 로그인은 기본적으로 `/admin` 운영 콘솔로 이동하며, 일반 회원에게만 회원 프로필의 게시글·댓글·반려동물 공개 범위 설정이 표시된다.

계정 UUID도 fixture에 고정되어 있다.

| 이메일 | UUID |
| --- | --- |
| `demo-member-1@townpet.local` | `00000000-0000-4000-8000-000000000201` |
| `demo-member-2@townpet.local` | `00000000-0000-4000-8000-000000000202` |
| `demo-member-3@townpet.local` | `00000000-0000-4000-8000-000000000203` |
| `demo-moderator@townpet.local` | `00000000-0000-4000-8000-000000000204` |

## 로컬 실행

실행 방식별 Docker·IntelliJ 명령은 [로컬 서버 실행 workflow](./local-development-workflow.md)를 따른다. 백엔드가 migration을 완료하고 PostgreSQL이 healthy인 뒤 아래 명령을 실행하면 fixture만 주입한다. 이 스크립트는 Docker 서비스 시작이나 이미지 빌드를 수행하지 않는다.

```bash
./scripts/seed-local-demo.sh
```

백엔드는 `http://localhost:8080`, PostgreSQL은 `localhost:54329`, MinIO 콘솔은 `http://localhost:9001`에서 확인할 수 있다. 프론트엔드는 별도 터미널에서 실행한다.

```bash
cd frontend
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

프론트엔드 개발 서버는 보통 `http://localhost:5173`이다. `/login`에서 위 계정으로 로그인한 다음 아래 화면을 확인한다.

- `/feed/guest`: 커뮤니티 샘플 글 2개, 댓글 3개(대댓글 1개), 좋아요 2개, 북마크 1개
- `/marketplace`: 판매·나눔 샘플 2개
- `/lost-found`: 분실·발견 샘플 2개와 목격 제보 2개
- `/boards/adoption`: 입양 샘플 2개
- `/volunteer`: 봉사 기회 2개와 지원 1개
- `/hospital-reviews`: 동물병원 후기 2개
- `/gatherings`: 산책·질문 모임 2개와 참여자 1명
- `/care`: 열린 돌봄 요청 1개, 매칭된 요청 1개와 지원·후기 흐름
- `/admin`: 운영 관리자 계정으로 신고·운영 기능 확인

`migration/fixtures/local-demo.sql`은 fixture가 소유한 고정 UUID와 두 fixture 게시글의 상호작용만 먼저 지운 뒤 다시 넣으므로 같은 명령을 반복해도 다른 로컬 데이터는 건드리지 않는다. PostgreSQL volume 자체를 비운 경우에는 먼저 PostgreSQL과 백엔드를 실행해 Flyway migration을 완료한 뒤 동일한 명령으로 계정과 샘플 데이터를 복원한다.

```bash
docker compose -f deploy/compose/local.yml down -v
# 방법 A 또는 방법 B로 PostgreSQL과 백엔드를 먼저 실행한 뒤
./scripts/seed-local-demo.sh
```

`down -v`는 로컬 PostgreSQL·MinIO volume의 모든 데이터를 삭제하므로, 개인적으로 추가한 테스트 데이터가 필요하면 실행하지 않는다.
