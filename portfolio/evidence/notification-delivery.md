# Notification delivery evidence

## 선택

원거래와 알림 저장을 분리하고 Spring Modulith Event Publication Registry를 사용한다.
listener는 at-least-once로 재전달될 수 있으므로 notification의 `event_id`를 database
unique key로 둔다. consumer는 `INSERT ... ON CONFLICT (event_id) DO NOTHING`의 영향 행 수로
신규 저장(1)과 중복 재전달(0)을 구분한다.

모든 `DataIntegrityViolationException`을 duplicate로 삼던 구현은 제거했다. 제목 길이,
FK, not-null 등 다른 무결성 오류는 예외로 남아 publication 실패와 재처리 신호가 된다.

## 검증

```bash
./gradlew test --tests com.townpet.notification.NotificationEventHandlerTest
./gradlew integrationTest --tests com.townpet.notification.NotificationEventHandlerIntegrationTest
./gradlew test --tests com.townpet.operations.EventPublicationRecoveryTest
./gradlew integrationTest --tests com.townpet.operations.EventPublicationRecoveryIntegrationTest
```

통합 테스트는 다음을 PostgreSQL에서 확인한다.

- 실제 `ApplicationEventPublisher` transaction 경로로 같은 `event_id`를 가진
  publication을 순차 재전달해도 notification 행은 1개이고, 해당 event의
  publication 2개가 각각 completion 된다.
- 같은 `event_id`를 8개 transaction에서 동시에 publish해도 notification 행은
  1개이고, 해당 event의 publication 8개가 각각 completion 된다.
- transaction 안에서 실제 `ApplicationEventPublisher`로 발행한 event가
  대상 event의 `event_publication.completion_date` 완료 상태로 수렴한다.
- listener가 비동기이므로 대상 `event_id`의 notification 저장·publication 완료를
  bounded polling 후 조회하며, 전체 event publication count를 사용하지 않는다.
- listener 실패로 `FAILED`가 된 publication에 recipient를 복구한 뒤 서버 재시작 없이
  recovery를 호출해 해당 publication이 완료되고 notification row가 1개로 남는다.
  실패는 존재하지 않는 recipient FK로 재현하고, recipient를 복구한 뒤 같은
  publication id의 status/completion/attempts를 확인한다.

## Recovery 경계

`EventPublicationRecovery`는 `TOWNPET_EVENT_RECOVERY_ENABLED=true`일 때 fixed-delay로
동작한다. netcup env 예시는 이를 활성화한다. 최소 age, batch size, max in-flight를
적용하고 `event_publication.completion_attempts`가 기본 3회 미만인 incomplete publication만
재전달한다. 따라서 `FAILED`라는 status만으로 자동 재전달을 제외하지 않지만, 시도 상한을
넘은 publication은 자동 재전달하지 않는다. backlog count와 oldest age metric도 상한을
넘은 publication을 제외한다. event payload·recipient·credential은 metric/log에 노출하지
않는다.

이 정책은 예외 종류를 자동으로 영구/일시 실패로 판정하지 않는다. 일시적 실패는 다음
재전달에서 완료될 수 있고, 반복 실패는 3회 상한 뒤 운영자가 원인을 확인하고 별도 조치를
취해야 한다. 실제 SMTP provider deliverability나 운영자 조치 시간은 이 테스트 범위가 아니다.

`NotificationEventHandlerTest`의 repository 예외 테스트는 listener exception이 duplicate
결과로 숨겨지지 않는다는 단위 경계를 확인한다. 실제 registry publication의 실패·재처리는
`EventPublicationRecoveryIntegrationTest`가 PostgreSQL에서 별도로 확인한다.

2026-09-09 targeted gate는 Testcontainers PostgreSQL에서 `EventPublicationRecoveryIntegrationTest`
의 recipient 누락→`FAILED`→recipient 복구→동일 publication completion 흐름을 통과했다.
`EventPublicationRecoveryTest`는 max-attempts 초과 publication 제외와 recovery 중복 실행 skip도
확인한다. 두 경로 모두 application restart 없이 같은 JVM/test context에서 completion을 확인한다.

2026-09-09 release `e6f7233`에서 netcup Compose와 운영 env에 bounded recovery 설정 7개를
주입했고, VPS `townpet-backend` container가 이를 수신한 상태로 healthy/readiness healthcheck를
통과했다. 이는 recovery worker가 운영 환경에서 활성화된 evidence다. 실제 외부 SMTP 장애를
운영 DB에서 유발해 복구한 결과나 provider deliverability를 의미하지는 않는다.

## 근거

- `src/main/java/com/townpet/notification/NotificationRepository.java`
- `src/main/java/com/townpet/notification/NotificationEventHandler.java`
- `src/main/java/com/townpet/operations/EventPublicationRecovery.java`
- `src/main/resources/application.yml`
- `deploy/netcup.env.example`
- `src/main/resources/db/migration/V066__notification_dedup.sql`
- `src/test/java/com/townpet/notification/NotificationEventHandlerIntegrationTest.java`
- `src/test/java/com/townpet/operations/EventPublicationRecoveryTest.java`
- `src/test/java/com/townpet/operations/EventPublicationRecoveryIntegrationTest.java`
