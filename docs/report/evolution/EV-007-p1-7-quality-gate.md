# EV-007 · P1.7 frontend·backend 통합 smoke와 CI quality gate

## 문제와 한계

backend와 frontend를 각각 통과시켜도 실제 통합에서는 Java version, frontend dependency, preview server, health endpoint 연결이 어긋날 수 있다. PR에서 로컬 개발자가 실행한 검증만 믿으면 fresh runner의 재현성과 main 보호가 보장되지 않는다.

## 대안과 결정

- 모든 검증을 하나의 거대한 workflow로 실행하는 방식은 실패 원인과 재실행 범위가 불명확해 제외했다.
- Vercel preview 중심 검증은 Spring Boot smoke와 Java toolchain 증거를 포함하지 못해 제외했다.
- backend, frontend, integration smoke를 별도 job으로 분리하고 PR에서는 병렬 quality + 통합 smoke, main에서는 quality + Chromium browser smoke를 실행하도록 결정했다.

## 구현

`pr.yml`은 Java 25 backend `clean check`, Node 22 frontend frozen install/typecheck/unit/build, 그리고 `frontend-backend-smoke.sh`를 실행한다. `main.yml`은 같은 quality gate 뒤 Chromium Playwright smoke를 추가한다. smoke script는 Spring `smoke` profile(H2 developmentOnly)의 Actuator health와 Vite preview HTML의 TownPet marker를 확인하고 항상 두 프로세스를 정리한다.

## 검증 결과

로컬에서 `./scripts/frontend-backend-smoke.sh`가 `frontend-backend smoke passed`를 출력했다. Spring `clean check`와 frontend typecheck·unit·build·E2E도 통과했다. smoke profile은 H2가 runtime classpath에 없어 처음 실패했으나 `developmentOnly`로 수정해 production artifact에 DB가 섞이지 않게 했다.

## 면접 답변

“검증을 backend/frontend 단위와 실제 통합 smoke로 분리했습니다. PR은 빠른 병렬 gate를, main은 browser smoke를 추가합니다. 로컬에서 재현한 동일 shell script를 CI가 실행하므로 환경 차이를 줄이고, 실패 시 어느 계층에서 깨졌는지 바로 알 수 있습니다.”
