# 공개 포트폴리오 데모 데이터

TownPet 공개 배포는 실제 회원가입을 받지 않고, 방문자가 로그인·게시글·댓글·답글·좋아요·북마크를 확인할 수 있도록 합성 계정과 콘텐츠를 제공한다.

## 공개 계정

| 역할 | 이메일 | 비밀번호 |
|---|---|---|
| MEMBER | `demo-member-1@townpet.local` | `townpet-demo-123!` |
| MEMBER | `demo-member-2@townpet.local` | `townpet-demo-123!` |
| MEMBER | `demo-member-3@townpet.local` | `townpet-demo-123!` |
| MODERATOR | `demo-moderator@townpet.local` | `townpet-moderator-123!` |

ADMIN·OPERATOR 자격은 공개하지 않는다. 계정은 실제 이메일 주소가 아니며, 공개 showcase 외의 환경에서 재사용하지 않는다.

## 포함되는 합성 데이터

- 게시글 50개(동물·자유게시판·질문·사진 자랑·용품 후기)
- 댓글·답글 387개
- 좋아요 1,178개로 구성된 HOT 목록
- 북마크, 장터, 입양, 분실·발견, 모임, 봉사, 병원 후기, 돌봄 요청

## VPS에서 콘텐츠 갱신

fixture 파일을 저장소의 최신 버전으로 VPS에 복사한 뒤 아래처럼 실행한다.

```bash
cd /opt/townpet
./scripts/seed-portfolio-demo.sh
```

이 스크립트는 PostgreSQL이 healthy인지 확인한 뒤 `local-demo.sql`과 `local-community-demo.sql`을 순서대로 실행한다. 두 파일은 고정된 demo ID 범위만 삭제하고 다시 생성하므로 전체 DB를 비우지 않는다. 자동 cron reset은 사용하지 않는다.

## 운영 주의

- fixture에는 실제 개인정보·실제 미디어·실제 연락처를 넣지 않는다.
- 방문자에게 공개되는 것은 MEMBER와 제한된 MODERATOR뿐이다.
- 데모 계정으로 생성한 추가 데이터는 정기적인 fixture 재실행 시 정리될 수 있다.
- 데이터베이스 백업과 fixture 재실행은 별도 절차다. fixture 실행 전에 운영 백업을 대체한다고 간주하지 않는다.
