# PLAN.md

## Goal

배포 전 TownPet의 실제 기능·보안·운영 경계를 완성한다. 공개 환경은 실제 개인정보와 공개 demo 계정을 사용하지 않는 portfolio sandbox로 유지한다. SMTP 계정 복구와 private media는 실제 production 경로로 구현하고, Redis·Kafka 같은 확장은 측정된 병목이 생길 때만 검토한다.

## Active

No active work. 저장소 내부 문서 정합성·품질 하드닝·보안 리뷰와 회귀 검증을 완료했다. 외부 인프라 작업은 이 저장소의 완료 기준에 포함하지 않는다.

## Backlog (trigger가 생길 때만)

- Redis: DB/cache/session 병목이 반복 재현될 때
- Kafka: PostgreSQL event publication으로 감당할 수 없는 외부 consumer·처리량이 생길 때
- Elasticsearch/SearchDocument: 검색 corpus·latency·정확도가 PostgreSQL 기준을 넘을 때
- 개인화 ranking projection: 실제 ranking 요구와 refresh 비용이 생길 때
- Kubernetes/microservice, 고급 WAL/PITR/HA, social login, 실제 public signup
- Marketplace 안전 규칙: public listing과 실제 사용자 입력을 열 때
- Hetzner 실제 DNS/TLS 배포·SMTP deliverability·MinIO public CORS·offsite backup: 실제 공개 배포를 시작할 때
- SEO/OG·SSR public metadata: 실제 검색 유입을 제품 범위로 확정할 때
- OPERATOR·ADMIN/MFA와 고급 projection/recovery: 공개 운영 범위와 규모가 확정될 때

## Working rules

- 기능은 작은 파일 단위가 아니라 충분한 vertical slice로 진행한다.
- 구현 중에는 가장 가까운 컴파일·기능 테스트만 실행하고, 큰 phase 종료 때 full gate를 실행한다.
- 중요한 결정·실패 원인·재현 가능한 수치만 `docs/report/`에 기록한다.
- 적용된 Flyway migration은 수정하지 않고 새 migration 또는 명시적 운영 스크립트를 추가한다.
