# 부하 시나리오 카탈로그

이 문서는 실제 k6 스크립트를 만들기 전의 시나리오 기준선이다. 정확한 URL·request body·response assertion은 [docs/parity/matrix.yaml](../parity/matrix.yaml), Spring controller DTO, `frontend/src/api/client.ts`에서 확인해 고정한다.

## 공통 규칙

- 모든 시나리오는 전용 performance DB와 합성 계정을 사용한다.
- session·CSRF·RBAC를 우회하는 내부 service 호출을 사용하지 않는다.
- 각 VU는 가능한 한 독립 계정을 사용한다. 같은 resource를 공유하는 경우는 contention 시나리오로 명시한다.
- 생성 요청은 fixture namespace 또는 고유 suffix를 사용해 우연한 unique 충돌을 피한다.
- 예상된 conflict는 기능 규칙에 맞는지 확인하고, 단순히 status 2xx만 성공으로 세지 않는다.
- 테스트가 끝나면 핵심 DB row count·status·version을 대사한다.

## S0 - Smoke와 계약 확인

### 목적

부하 전에 인증·CSRF·routing·DB 연결·응답 계약이 정상인지 확인한다.

### 흐름

1. health/readiness 요청
2. anonymous public GET
3. perf MEMBER 로그인과 CSRF 획득
4. 보호된 member GET
5. moderator 로그인과 admin GET
6. 대표 write 1건과 cleanup

### 판정

- unexpected 5xx·timeout 없음
- session cookie와 CSRF 계약 정상
- 역할별 401/403/200 결과가 권한 매트릭스와 일치
- 생성 row가 cleanup 후 남지 않음

## S1 - Public Read

### 목적

비회원 중심의 목록·상세·검색 읽기 비용과 index/pagination 효과를 측정한다.

### 요청군

- public feed와 게시글 상세
- catalog·breed·neighborhood
- local guide 검색과 상세
- adoption·volunteer·hospital 공개 목록
- 공개 lost-found 목록과 근사 위치 응답

### 초기 workload

```text
public list        40%
publication detail 20%
catalog/guide      20%
welfare/lostfound  20%
```

각 목록은 Small → Medium 순서로 실행한다. 응답 item 수, cursor, 빈 검색 결과도 assertion한다.

### 핵심 지표

- p50/p95/p99
- query rows와 buffer hit
- 목록 limit 준수
- N+1 또는 반복 query
- exact location·private field 노출 여부

## S2 - Member Read

### 목적

session 조회와 회원별 상태가 추가된 뒤 public read와 비교한다.

### 요청군

- member feed와 본인 profile/pet
- notification unread/all
- bookmark와 member activity
- follow/block 상태
- care·gathering·marketplace의 본인/공개 목록

### 초기 workload

```text
member feed/profile 35%
notification         20%
relationship         15%
bookmark/activity    15%
domain lists         15%
```

같은 session을 재사용하는 흐름과 VU별 계정을 바꾸는 흐름을 구분한다. 다른 회원 ID를 URL에 넣어도 본인 데이터가 나오지 않는지 확인한다.

## S3 - Write Burst

### 목적

일반적인 write 요청의 transaction 비용과 retry·idempotency 결과를 확인한다.

### 요청군

- publication create/edit/delete
- comment create/delete
- reaction·bookmark active/inactive
- relationship follow/block
- care request/application
- marketplace listing

### 규칙

- 각 VU는 고유한 제목·body·resource를 사용한다.
- reaction·bookmark는 같은 요청을 두 번 보내 멱등성을 확인하는 별도 sub-case를 둔다.
- write 성공 후 read-back하여 저장 상태와 response가 일치하는지 확인한다.
- validation 실패와 ownership 실패는 예상된 4xx로 분류한다.

## S4 - Contention과 상태 경합

### 목적

단순 throughput이 아니라 동시에 같은 row를 변경할 때 데이터가 안전한지 확인한다.

### 경합 case

| Case | 동시 요청 | 성공 조건 |
|---|---|---|
| publication metric | 같은 게시글 조회수 증가 | 최종 count가 요청 수와 일치 |
| volunteer capacity | 정원보다 많은 신청 | accepted row가 capacity 이하, 초과 요청은 결정적 409 |
| follow/block | 같은 viewer-target에 follow·block | 상충 원장이 동시에 남지 않음 |
| trust report | 같은 target 신고 | open case unique 정책 유지 |
| care accept | 같은 request의 여러 application 수락 | assignment 단일성·version 증가 |
| media attach | 같은 publication 동시 첨부 | 허용 개수 초과 없음 |

### 판정

- lost update, duplicate row, capacity 초과, deadlock 없음
- 예상된 conflict의 code/status가 안정적
- 최종 DB 대사 결과가 HTTP 응답 수보다 우선

## S5 - Moderator/Admin

### 목적

운영 큐와 대량 moderation이 일반 사용자 traffic을 방해하지 않는지 확인한다.

### 요청군

- trust report queue
- moderator case queue
- bulk report review
- member content hide/restore
- auth audit·moderation action·policy 조회

### workload

- queue GET 80%
- 단건 review 15%
- bulk review/hide/restore 5%

bulk ID는 1·10·100건으로 나누어 실행한다. 존재하지 않는 ID가 섞였을 때 응답의 실제 affected count가 맞는지 확인한다.

## S6 - Media I/O

### 목적

DB query 성능과 filesystem/object adapter 비용을 분리한다.

### 흐름

1. upload metadata 발급
2. 허용 MIME·크기 파일 전송
3. finalize
4. publication attachment
5. 만료 upload cleanup

### 별도 측정

- metadata API latency
- upload byte throughput
- finalize·checksum·magic-byte 검사 시간
- DB connection 점유시간
- filesystem 용량과 cleanup 처리량

실제 object storage가 도입되기 전에는 local adapter 결과를 production object storage 성능으로 표현하지 않는다.

## S7 - Mixed Workload

### 목적

개별 시나리오가 통과해도 서로 섞였을 때 pool·lock·CPU가 포화되는지 확인한다.

### 초기 혼합 비율

```text
public read  55%
member read  25%
write        15%
moderator     5%
```

이 비율은 실제 사용자 통계가 없는 상태의 합성 가정이다. baseline 보고서에 반드시 가정으로 표시한다.

## S8 - Soak와 Spike

### Soak

S7에서 안정적으로 통과한 VU 수준을 30~60분 유지한다.

확인 항목:

- p95가 시간에 따라 악화되는지
- heap/RSS와 GC가 증가하는지
- DB pool·connection·lock wait가 누적되는지
- event publication과 cleanup backlog가 쌓이는지

### Spike

1~2분의 낮은 부하에서 목표 VU로 급증시킨 뒤 원래 부하로 낮춘다.

확인 항목:

- timeout과 5xx가 회복되는지
- pool이 정상 수준으로 돌아오는지
- 실패한 write가 partial state를 남기지 않는지
- readiness가 거짓 양성을 내지 않는지

## 실행 순서

```text
S0
→ S1 Small/Medium
→ S2 Small/Medium
→ S3
→ S4
→ S5
→ S6
→ S7
→ S8
```

S1~S2 baseline과 병목 확인 전에는 Redis·Kafka를 추가하지 않는다. 확장 기술 비교는 동일 시나리오와 동일 fixture를 기준으로 [redis-kafka-evaluation.md](redis-kafka-evaluation.md)에 기록한다.
