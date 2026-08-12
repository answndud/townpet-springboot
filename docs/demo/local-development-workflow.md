# 로컬 서버 실행 workflow

이 문서는 `/Users/alex/project/townpet-springboot`에서 Docker 백엔드와 React/Vite 프론트엔드를 함께 실행하는 순서다. Docker Desktop이 먼저 실행 중이어야 한다.

## 1. 프로젝트 폴더로 이동

새 터미널을 열고 프로젝트 루트에서 시작한다.

```bash
cd /Users/alex/project/townpet-springboot
```

이후 명령은 별도 표시가 없으면 모두 이 폴더에서 실행한다.

## 2. Docker 백엔드와 PostgreSQL 시작

첫 번째 터미널에서 실행한다.

```bash
./scripts/seed-local-demo.sh
```

이 명령은 다음을 한 번에 처리한다.

1. PostgreSQL, Spring Boot 백엔드, MinIO 컨테이너를 시작한다.
2. Docker 이미지가 없거나 코드가 변경됐으면 백엔드 이미지를 다시 빌드한다.
3. Spring Boot가 Flyway migration을 적용할 때까지 기다린다.
4. 로컬 데모 계정과 게시판 샘플 데이터를 PostgreSQL에 넣는다.

정상 완료 시 `Local demo data is ready`라는 메시지가 출력된다. 이 터미널은 로그를 확인할 수 있도록 열어 둔다.

백엔드 API 주소는 [http://localhost:8080](http://localhost:8080)이며, PostgreSQL은 `localhost:54329`, MinIO 콘솔은 [http://localhost:9001](http://localhost:9001)이다.

## 3. 프론트엔드 개발 서버 시작

두 번째 터미널을 열고 같은 프로젝트 폴더로 이동한다.

```bash
cd /Users/alex/project/townpet-springboot
corepack pnpm -C frontend install --frozen-lockfile
corepack pnpm -C frontend dev
```

의존성이 이미 설치되어 있다면 두 번째 줄은 생략해도 된다. 정상 실행 시 Vite가 [http://localhost:5173](http://localhost:5173)에서 대기한다.

프론트엔드의 `/api` 요청은 Vite proxy를 통해 `http://localhost:8080` 백엔드로 전달된다. 따라서 브라우저 테스트는 [http://localhost:5173](http://localhost:5173)에서 한다.

## 4. 브라우저에서 테스트

1. [http://localhost:5173/login](http://localhost:5173/login)을 연다.
2. [로컬 데모 계정 문서](./local-demo-accounts.md)의 일반 사용자 또는 운영 관리자 계정으로 로그인한다.
3. 다음 순서로 주요 흐름을 확인한다.

| 테스트 대상 | 화면 |
| --- | --- |
| 커뮤니티 글·댓글·대댓글·좋아요·북마크 | `/feed/guest` |
| 거래 글 | `/marketplace` |
| 분실·발견 및 목격 제보 | `/lost-found` |
| 입양 정보 | `/boards/adoption` |
| 봉사 지원 | `/volunteer` |
| 병원 후기 | `/hospital-reviews` |
| 모임 참여 | `/gatherings` |
| 돌봄 지원·수락·상태 전이 | `/care` |
| 운영자 신고·운영 로그 | `/admin` |

## 5. 종료

프론트엔드 터미널에서 `Ctrl+C`를 눌러 Vite를 종료한다. Docker 서비스는 세 번째 터미널에서 다음처럼 종료한다.

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml down
```

`down`은 컨테이너만 종료하고 PostgreSQL·MinIO 데이터 volume은 보존한다. 다음 실행 때 같은 계정과 데이터가 남아 있다.

## 6. 데이터까지 초기화하고 다시 채우기

샘플 데이터와 로컬에서 만든 테스트 데이터를 모두 지우고 처음 상태로 되돌릴 때만 실행한다.

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml down -v
./scripts/seed-local-demo.sh
```

`down -v`는 PostgreSQL·MinIO volume을 삭제하는 명령이므로 개인적으로 추가한 테스트 데이터도 함께 사라진다.

## 문제 해결

- `port is already allocated`: 8080, 5173, 54329, 9000, 9001 포트를 사용 중인 프로세스를 종료한 뒤 다시 실행한다.
- Docker 서비스가 healthy가 되지 않음: 첫 번째 터미널에서 `docker compose -f deploy/compose/local.yml logs backend postgres`로 로그를 확인한다.
- 프론트엔드에서 API가 실패함: 첫 번째 터미널의 백엔드가 healthy인지 확인하고 브라우저 주소가 `localhost:5173`인지 확인한다.
- 계정·샘플 데이터가 보이지 않음: 프로젝트 루트에서 `./scripts/seed-local-demo.sh`를 다시 실행한다.
