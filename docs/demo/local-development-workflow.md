# 로컬 서버 실행 workflow

이 문서는 `/Users/alex/project/townpet-springboot`에서 PostgreSQL·Spring Boot·React/Vite를 원하는 방식으로 나눠 실행하는 절차다. Docker Desktop이 필요한 구성에서는 먼저 실행해 둔다.

## 공통 준비

새 터미널에서 프로젝트 루트로 이동한다.

```bash
cd /Users/alex/project/townpet-springboot
```

명령은 별도 표시가 없으면 모두 이 폴더에서 실행한다. 프론트엔드는 항상 별도 터미널에서 실행한다.

## 방법 A: Docker로 PostgreSQL만 실행하고 IntelliJ에서 백엔드 실행

IntelliJ에서 Java 디버깅·코드 수정·재시작을 빠르게 반복할 때 사용하는 방법이다.

### 터미널 1 — 의존 서비스만 시작

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml up -d postgres
docker compose -f deploy/compose/local.yml ps
```

`postgres`가 `healthy`가 될 때까지 기다린다. 이 단계에서는 Docker의 `backend` 서비스를 시작하지 않는다.

### IntelliJ — Spring Boot 실행

`src/main/java/com/townpet/TownPetApplication.java`를 열고 실행한다. Run Configuration에는 다음 값을 지정한다.

```text
Active profiles: local
TOWNPET_DB_URL: jdbc:postgresql://localhost:54329/townpet
TOWNPET_DB_USERNAME: townpet_app
TOWNPET_DB_PASSWORD: townpet_local_dev
TOWNPET_DEMO_DATA_ENABLED: true
```

애플리케이션 로그에 Flyway migration 완료와 `Tomcat started on port 8080`이 표시되면 백엔드가 준비된 것이다. IntelliJ 실행 환경에는 `application-local.yml`의 기본값이 있지만, 위 값을 Run Configuration에 명시하면 환경 차이를 줄일 수 있다.

### 터미널 2 — 데모 데이터 주입

백엔드가 한 번 시작되어 migration을 적용한 뒤에만 실행한다.

```bash
cd /Users/alex/project/townpet-springboot
./scripts/seed-local-demo.sh
```

이 스크립트는 서비스를 시작하거나 이미지를 빌드하지 않는다. 실행 중인 PostgreSQL에 fixture만 주입한다.

## 방법 B: 백엔드까지 Docker로 실행

IntelliJ를 사용하지 않고 컨테이너로 백엔드를 실행할 때 사용하는 방법이다.

### 터미널 1 — 필요한 Docker 서비스만 선택해 시작

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml up -d postgres
docker compose -f deploy/compose/local.yml up -d --build backend
docker compose -f deploy/compose/local.yml ps
```

`backend`가 `healthy`가 되면 Flyway migration도 완료된 상태다. 현재 `local` 프로필은 파일 저장소를 사용하므로 MinIO는 필수가 아니다. MinIO 콘솔까지 확인할 때만 `docker compose -f deploy/compose/local.yml up -d minio`를 별도로 실행한다. 이후 데모 데이터를 채운다.

```bash
./scripts/seed-local-demo.sh
```

이미지가 이미 있고 코드 변경도 없다면 `--build`를 생략해도 된다.

## 방법 C: Docker와 IntelliJ/Docker 서비스를 이미 켜 둔 경우

먼저 현재 상태만 확인한다.

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml ps
```

- PostgreSQL이 `healthy`이고 IntelliJ 백엔드가 8080에서 실행 중이면 `./scripts/seed-local-demo.sh`만 실행한다.
- PostgreSQL만 실행 중이면 IntelliJ에서 `TownPetApplication`을 실행한 뒤 fixture 스크립트를 실행한다.
- Docker `backend`까지 `healthy`이면 별도 IntelliJ 백엔드를 실행하지 말고 fixture 스크립트만 실행한다.
- PostgreSQL이 없으면 방법 A 또는 방법 B의 PostgreSQL 시작 명령만 실행한다.

## 방법 D: 프론트엔드 실행

세 번째 터미널에서 실행한다. 방법 A·B·C 중 어떤 백엔드를 선택했든 백엔드가 `localhost:8080`에서 실행 중이면 동일하다.

```bash
cd /Users/alex/project/townpet-springboot
corepack pnpm -C frontend install --frozen-lockfile
corepack pnpm -C frontend dev
```

의존성이 이미 설치되어 있으면 `install`은 생략한다. Vite는 [http://localhost:5173](http://localhost:5173)에서 실행되고 `/api` 요청은 `http://localhost:8080`으로 proxy된다.

## 브라우저 테스트

1. [http://localhost:5173/login](http://localhost:5173/login)을 연다.
2. [로컬 데모 계정 문서](./local-demo-accounts.md)의 계정으로 로그인한다.
3. 다음 화면을 확인한다.

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

## 종료

Vite 터미널과 IntelliJ 실행을 각각 중지한다. Docker 서비스는 필요한 범위에 따라 종료한다.

```bash
cd /Users/alex/project/townpet-springboot
# 컨테이너만 종료하고 volume은 보존
docker compose -f deploy/compose/local.yml down
```

개인 테스트 데이터까지 초기화할 때만 다음을 실행한다.

```bash
docker compose -f deploy/compose/local.yml down -v
```

`down -v`는 PostgreSQL·MinIO volume을 삭제한다. 이후 다시 사용할 때는 방법 A 또는 B로 서비스를 시작하고, 백엔드 migration 완료 후 `./scripts/seed-local-demo.sh`를 실행한다.

## 문제 해결

- `port is already allocated`: 8080, 5173, 54329, 9000, 9001 포트를 사용하는 다른 프로세스를 종료한다.
- `seed-local-demo.sh`가 `PostgreSQL is not running`을 출력함: 먼저 방법 A 또는 B의 `postgres` 시작 명령을 실행한다.
- `Flyway schema is not ready`를 출력함: IntelliJ 또는 Docker `backend`를 한 번 실행해 migration을 완료한 뒤 다시 실행한다.
- Docker 서비스가 healthy가 되지 않음: `docker compose -f deploy/compose/local.yml logs backend postgres`로 로그를 확인한다.
- 프론트엔드 API 실패: 백엔드가 실제로 `localhost:8080`에서 실행 중인지, 브라우저 주소가 `localhost:5173`인지 확인한다.
