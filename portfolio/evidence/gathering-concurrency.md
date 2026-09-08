# Gathering concurrency evidence

## 문제와 재현

정원 3명 모임에 서로 다른 10명의 회원이 동시에 참가하면, 요청이 정원을
확인한 뒤 저장하는 순서가 엇갈려 정원 초과가 발생할 수 있다. 같은 회원이
중복 신청하면 별도의 중복 행이 생길 수 있다.

회귀 테스트는 두 상황을 분리한다.

- `GatheringControllerTest.concurrentJoinsNeverExceedCapacityOrDuplicateParticipant`
  는 10명의 서로 다른 principal을 latch 뒤에 실행해 200 응답 3개, 409 응답
  7개, 5xx 0개와 최종 participant 3명을 확인한다.
- `GatheringControllerTest.concurrentDuplicateJoinsForSameMemberCreateOneParticipant`
  는 같은 principal의 동시 10회 신청과 최종 DB 행 1개를 확인한다.

검증 명령:

```bash
./gradlew integrationTest --tests com.townpet.gathering.GatheringControllerTest
```

## 선택과 역할 분리

`GatheringService.join`은 `GatheringRepository.findForUpdate`로 모임 부모 행을
비관적 잠금한다. 따라서 같은 모임의 정원 확인과 participant insert가 한 번에
직렬화되고, 정원이 차면 후속 요청은 409로 끝난다.

`V022__gathering.sql`의
`gathering_participant_uq (gathering_id, member_id)`는 같은 회원의 중복 행을
막는 database-level invariant다. 애플리케이션의 사전 조회는 정상적인
idempotent 응답을 만들고, unique constraint는 동시 실행에서도 최종 상태를
보호한다.

두 장치는 서로 대체하지 않는다. 부모 row lock은 정원이라는 aggregate-wide
불변식을 보호하고, unique constraint는 한 회원의 membership 중복만 보호한다.

## Trade-off와 한계

목록 조회의 SQL statement 수에 대한 별도 수치(`N+1 → N` 등)는 현재 공개하지
않는다. `GatheringService.list`는 모임 목록 후 participant count를
`countByGatheringIdIn` 한 번으로 집계한다는 코드 근거만 기록하며, datasource 또는
Hibernate statement-count 측정 artifact가 없는 상태에서 숫자 개선률을 주장하지
않는다.

- row lock은 같은 모임에 대한 참가 요청을 직렬화하므로 contention이 높으면
  대기 시간이 늘어난다.
- 조건부 `UPDATE ... WHERE participant_count < capacity`는 별도 counter 관리와
  참가자 행 정합성 대사가 필요하다.
- optimistic locking은 충돌 시 재시도·409 정책이 필요하며, 현재 제품의
  작은 모임 참가 흐름에는 부모 row lock이 더 직접적인 선택이다.
- 이 테스트는 애플리케이션 단일 인스턴스가 아니라 PostgreSQL의 실제 잠금·FK·
  unique 동작을 사용하지만, 운영 트래픽의 최대 처리량을 주장하지 않는다.
