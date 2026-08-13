# PLAN.md

## Goal

배포를 시작하기 전에 저장소 안에서 완성할 수 있는 기능·보안·복구·관측·성능 검증을 모두 닫아 **production release candidate**를 만든다. 실제 VPS의 DNS/TLS, 외부 SMTP 발송, 외부 백업 보관과 공개 트래픽 측정은 배포 단계에서 수행하되, 그 절차와 실패 시 대응은 이 계획에서 재현 가능하게 준비한다.

## Active

No active work. P1~P3의 저장소 내부 작업과 최종 검증을 완료했고, frontend release polish까지 commit으로 고정했다. 실제 VPS·외부 provider 검증만 배포 단계의 backlog다.

## Backlog

- 실제 VPS DNS/TLS·Caddy forwarded-header·secure cookie·edge rate limit 검증
- 실제 SMTP provider TLS/SPF/DKIM/deliverability 검증
- 외부 failure domain에 backup 보관 후 restore와 RPO/RTO 측정
- 실제 VPS에서 동일 workload와 CPU/memory/disk/DB connection을 측정
- Redis/Kafka는 DB saturation, cache miss 병목, notification/projection backlog가 재현될 때만 별도 실험

## Working rules

- 한 slice는 기능·운영 경계가 연결된 vertical slice로 진행하고, 파일 단위 작업과 반복적인 전체 gate를 만들지 않는다.
- 구현 중에는 가장 가까운 컴파일·기능 검증만 실행하고, P3.2에서 전체 gate를 한 번 실행한다.
- report는 새로운 설계 판단·실패 원인·재현 가능한 수치가 생길 때만 갱신한다.
- 적용된 Flyway migration은 수정하지 않고 새 migration 또는 명시적 운영 스크립트를 추가한다.
- 실제로 실행하지 않은 외부 배포·백업·SMTP·성능 결과를 완료했다고 기록하지 않는다.
