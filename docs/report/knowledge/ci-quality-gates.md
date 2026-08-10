# 계층형 CI와 통합 smoke 면접 노트

## 계층

1. backend: Java 25, Gradle, Modulith·migration·OpenAPI·parity inventory 포함 `clean check`
2. frontend: frozen pnpm install, TypeScript, Vitest, production build
3. integration: Spring Actuator readiness와 Vite preview HTML을 같은 script에서 확인
4. browser: main에서 Playwright Chromium shell journey 실행

PR job은 backend/frontend를 병렬 실행하고 smoke는 둘 모두 성공한 뒤 실행한다. GitHub Actions permission은 `contents: read`만 부여한다.

## trade-off

PR마다 전체 browser와 Testcontainers를 모두 실행하면 피드백이 느려진다. 그래서 PR에서는 deterministic shell smoke, main에서는 browser smoke로 비용과 보호 수준을 분리했다. domain vertical slice가 추가되면 해당 parity E2E를 main/nightly 계층에 확장한다.

## 재현성 포인트

- Java 25와 Node 22를 workflow에서 명시한다.
- `pnpm-lock.yaml`과 Gradle dependency cache를 사용한다.
- smoke는 외부 OAuth나 production DB 없이 H2 smoke profile과 정적 Vite preview만 사용한다.
- 프로세스 PID와 `trap` cleanup으로 실패해도 runner를 오염시키지 않는다.
