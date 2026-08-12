# 로컬 서버 실행 workflow

이 문서는 Docker Desktop 앱은 이미 실행해 둔 상태에서 `/Users/alex/project/townpet-springboot`의 나머지 서비스를 모두 터미널 CLI로 실행하는 기본 절차다. IntelliJ 실행은 마지막에 선택적 대안으로 설명한다.

## 공통 준비

새 터미널에서 프로젝트 루트로 이동한다.

```bash
cd /Users/alex/project/townpet-springboot
```

명령은 별도 표시가 없으면 모두 이 폴더에서 실행한다. 프론트엔드는 항상 별도 터미널에서 실행한다.

## 기본 workflow: Docker Desktop + 터미널 CLI

Docker Desktop 앱을 먼저 열고, Docker 엔진이 실행된 뒤 새 터미널을 연다. Docker Desktop 자체를 CLI에서 다시 시작할 필요는 없다.

### 터미널 1 — Docker 상태와 PostgreSQL 확인

```bash
cd /Users/alex/project/townpet-springboot
docker info
docker compose -f deploy/compose/local.yml up -d postgres
docker compose -f deploy/compose/local.yml ps
```

`postgres`가 `healthy`가 될 때까지 기다린다. `docker info`가 실패하면 Docker Desktop이 아직 준비되지 않은 것이다.

### 터미널 1 — Docker 백엔드 실행

```bash
docker compose -f deploy/compose/local.yml up -d --build backend
docker compose -f deploy/compose/local.yml ps
```

`backend`가 `healthy`가 되면 Spring Boot 시작과 Flyway migration이 완료된 상태다. 이미지가 이미 있고 코드 변경도 없다면 `--build`를 생략해도 된다.

### 터미널 1 — 데모 데이터 주입

백엔드가 migration을 완료한 뒤에 실행한다.

```bash
./scripts/seed-local-demo.sh
```

이 스크립트는 서비스를 시작하거나 이미지를 빌드하지 않고, 실행 중인 PostgreSQL에 fixture만 주입한다.

### 터미널 2 — React/Vite 프론트엔드 실행

새 터미널을 열고 다음을 실행한다.

```bash
cd /Users/alex/project/townpet-springboot
corepack pnpm -C frontend install --frozen-lockfile
corepack pnpm -C frontend dev
```

의존성이 이미 설치되어 있으면 `install`은 생략한다. Vite는 [http://localhost:5173](http://localhost:5173)에서 실행되고 `/api` 요청은 `http://localhost:8080`으로 proxy된다.

### 터미널 3 — 선택적 로그 확인

서비스 로그를 보고 싶을 때만 별도 터미널에서 실행한다.

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml logs -f backend postgres
```

MinIO는 현재 `local` 프로필의 파일 저장소를 사용하므로 기본 실행에 필요하지 않다. MinIO 콘솔까지 확인할 때만 다음을 별도로 실행한다.

```bash
docker compose -f deploy/compose/local.yml up -d minio
```

## 브라우저 테스트

기본 workflow가 끝나면 브라우저에서 [http://localhost:5173/login](http://localhost:5173/login)을 연다.

[로컬 데모 계정 문서](./local-demo-accounts.md)의 계정으로 로그인한 뒤 다음 화면을 확인한다.

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

## 선택적 대안: IntelliJ에서 백엔드만 실행

코드 디버깅이 필요할 때는 Docker Desktop은 그대로 켜 둔 채 Docker PostgreSQL만 CLI로 실행한다.

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml up -d postgres
```

그 다음 IntelliJ에서 `src/main/java/com/townpet/TownPetApplication.java`를 실행하고, Run Configuration에 아래 값을 지정한다.

```text
Active profiles: local
TOWNPET_DB_URL: jdbc:postgresql://localhost:54329/townpet
TOWNPET_DB_USERNAME: townpet_app
TOWNPET_DB_PASSWORD: townpet_local_dev
TOWNPET_DEMO_DATA_ENABLED: true
```

로그에 Flyway migration 완료와 `Tomcat started on port 8080`이 표시되면 `./scripts/seed-local-demo.sh`를 실행하고, 프론트엔드는 기본 workflow와 같은 CLI 명령으로 실행한다.

## 종료

Vite 터미널과 실행 중인 IntelliJ 프로세스를 각각 중지한다. Docker 서비스는 필요한 범위에 따라 종료한다.

```bash
cd /Users/alex/project/townpet-springboot
# 컨테이너만 종료하고 volume은 보존
docker compose -f deploy/compose/local.yml down
```

개인 테스트 데이터까지 초기화할 때만 다음을 실행한다.

```bash
docker compose -f deploy/compose/local.yml down -v
```

`down -v`는 PostgreSQL·MinIO volume을 삭제한다. 이후 다시 사용할 때는 기본 CLI workflow 또는 IntelliJ 대안으로 PostgreSQL과 백엔드를 시작하고, migration 완료 후 `./scripts/seed-local-demo.sh`를 실행한다.

## 문제 해결

- `port is already allocated`: 8080, 5173, 54329, 9000, 9001 포트를 사용하는 다른 프로세스를 종료한다.
- `seed-local-demo.sh`가 `PostgreSQL is not running`을 출력함: 먼저 기본 CLI workflow 또는 IntelliJ 대안의 `postgres` 시작 명령을 실행한다.
- `Flyway schema is not ready`를 출력함: IntelliJ 또는 Docker `backend`를 한 번 실행해 migration을 완료한 뒤 다시 실행한다.
- Docker 서비스가 healthy가 되지 않음: `docker compose -f deploy/compose/local.yml logs backend postgres`로 로그를 확인한다.
- 프론트엔드 API 실패: 백엔드가 실제로 `localhost:8080`에서 실행 중인지, 브라우저 주소가 `localhost:5173`인지 확인한다.
