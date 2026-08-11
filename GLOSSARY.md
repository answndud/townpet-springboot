# GLOSSARY.md

## 기능 동등성

같은 입력과 권한 조건에서 기존 TownPet과 `townpet-springboot`가 사용자 또는 운영자에게 의미상 같은 결과를 제공하는 성질이다. 내부 SQL, 클래스 구조, API 구현 방식이 같은 것을 뜻하지 않는다.

## 제품 동작 기준선

기능 동등성을 판단하기 위해 고정하는 기존 TownPet의 코드, 데이터 계약, 화면과 테스트 상태다. 정확한 Git commit은 후속 결정에서 지정한다.

## 관찰 가능한 동작

URL, 화면 내용, 입력 규칙, 권한, HTTP 결과, 상태 전이, 오류·로딩·빈 상태, 알림과 관리자 처리처럼 시스템 밖에서 확인할 수 있는 결과다.

## 내부 재아키텍처

관찰 가능한 제품 동작은 유지하면서 서버 책임, 트랜잭션, 도메인 모델, 영속성, 보안과 운영 구조를 Spring Boot에 맞게 다시 설계하는 작업이다.

## 의도적 차이

보안, 일관성, 접근성, 성능 또는 운영성을 높이기 위해 기존 동작과 다르게 만들기로 명시적으로 선택한 부분이다. 반드시 ADR과 회귀 기준에 기록한다.

## 완료 증거

구현 여부가 아니라 테스트, API 계약 비교, 데이터 검증, 성능 측정, 배포 후 점검으로 해당 전환이 끝났음을 확인하는 자료다.

## 점진 전환

실행 가능한 기존 시스템을 유지한 채 도메인 하나씩 새 Spring 백엔드로 소유권을 옮기는 방식이다. 각 단계는 데이터, API, UI와 회귀 테스트를 함께 닫는다.

## Legacy Adapter

아직 Spring으로 전환되지 않은 기존 기능을 새 프런트엔드가 임시로 호출할 수 있게 하는 호환 계층이다. 영구 아키텍처가 아니며 해당 도메인 전환 완료 시 제거한다.

## Write Owner

특정 데이터의 생성·변경·삭제를 책임지는 단일 구현체다. 전환 중에도 같은 데이터에 Next.js와 Spring이 동시에 쓰지 않도록 도메인별 write owner를 하나만 둔다.

## 의미 보존 마이그레이션

원본과 새 스키마의 물리적 구조가 달라도 식별자, 관계, 상태, 시각과 비즈니스 의미가 유지되도록 데이터를 변환하는 작업이다.

## 멱등 마이그레이션

같은 입력으로 여러 번 실행해도 중복 레코드나 추가 상태 변경 없이 동일한 결과에 도달하는 마이그레이션이다.

## 마이그레이션 대사

이전 전후의 건수, 관계, 상태 분포, 집계와 표본 값을 비교해 누락·중복·의미 손실을 찾는 검증 과정이다.

## 결정적 익명화 Fixture

개인정보를 포함하지 않으면서 원본 도메인의 관계와 경계 사례를 매 실행에서 동일하게 재현하는 테스트·데모 데이터 세트다.

## 영속 원장

도메인 상태의 최종 진실을 보관하며 장애 후 복구와 대사의 기준이 되는 저장소다. TownPet에서는 PostgreSQL만 이 역할을 맡는다.

## 보조 저장소

캐시, rate limit, 단기 coordination처럼 원장으로부터 재구축하거나 손실을 허용할 수 있는 상태를 보관하는 저장소다. Redis를 사용하더라도 이 범위를 넘지 않는다.

## Read Repository

JPA aggregate 변경과 분리해 복잡한 목록, 검색, 집계와 최적화된 projection을 담당하는 조회 전용 경계다. 필요한 경우 명시적 SQL을 사용할 수 있다.

## 단일 서버 런타임

운영 요청을 처리하는 애플리케이션 서버를 Spring Boot/JVM 하나로 제한하는 배포 모델이다. 프런트엔드 빌드에 Node.js를 사용할 수 있지만 Node.js 서버는 운영하지 않는다.

## 정적 프런트엔드 Asset

빌드 시 생성되어 실행 중 서버 로직 없이 브라우저에 전달되는 JavaScript, CSS, 이미지와 글꼴 파일이다. Spring Boot 또는 정적 파일 계층이 제공할 수 있다.

## HTML Shell

브라우저 애플리케이션이 mount되는 최소 HTML 문서다. 공개 URL에서는 Spring MVC가 페이지별 title과 Open Graph metadata를 포함해 생성할 수 있다.

## 개발 Proxy

Vite 개발 서버가 브라우저의 `/api/**` 요청을 로컬 Spring Boot로 전달하는 개발 전용 경로다. 운영 아키텍처의 별도 gateway가 아니다.

## Client-side Routing

최초 HTML shell을 받은 뒤 브라우저에서 URL과 화면을 전환하는 방식이다. React Router가 기존 TownPet URL을 유지하고 direct URL은 Spring MVC가 shell로 연결한다.

## 프런트엔드 빌드 통합

Vite가 생성한 정적 asset을 Spring Boot의 실행 산출물 또는 배포 정적 계층에 포함해 같은 release로 검증하는 과정이다.

## 모듈형 모놀리스

하나의 프로세스와 배포 단위를 사용하지만 도메인별 공개 API, 내부 구현과 의존 방향을 명시적으로 분리하고 검증하는 아키텍처다.

## Application Module

Spring Modulith가 인식하는 기능 단위다. 다른 모듈에 제공하는 공개 interface와 event, 내부 구현, 허용된 의존성을 가진다.

## 모듈 공개 API

다른 application module이 동기적으로 호출하거나 참조할 수 있도록 의도적으로 노출한 타입과 유스케이스 계약이다. JPA entity와 repository는 포함하지 않는다.

## 모듈 이벤트

한 모듈에서 완료된 사실을 다른 모듈에 전달하는 immutable message다. 즉시 응답에 필수인 불변식보다 commit 이후 부수효과에 우선 사용한다.

## Publication

TownPet에서 공개 또는 제한 공개되는 모든 게시물의 공통 식별자, 작성 주체, 위치·커뮤니티 범위, 제목·본문과 게시 상태를 소유하는 aggregate다.

## 구조화 게시 Aggregate

`publicationId`를 통해 공통 게시물과 연결되며 분실·장터·돌봄 등 특정 업무의 필드, 불변식과 상태 전이를 소유하는 aggregate다.

## Aggregate

한 트랜잭션에서 불변식을 일관되게 지키는 도메인 객체 경계다. 다른 aggregate는 객체 연관관계 대신 식별자와 공개 유스케이스를 통해 참조한다.

## Read Model

쓰기 aggregate 구조와 독립적으로 피드, 검색, 상세와 관리자 목록에 필요한 데이터를 효율적으로 반환하도록 만든 조회 표현이다.

## 서버 세션

인증 상태를 서버 저장소에 보관하고 브라우저에는 상태를 직접 담지 않은 opaque identifier만 cookie로 전달하는 방식이다.

## Credentials

회원이 검증된 이메일과 비밀번호로 로그인하는 현재 TownPet의 유일한 인증 방식이다. Kakao·Naver 같은 social provider 인증은 현재 제품 범위가 아니다.

## 세션 폐기

로그아웃, 비밀번호 변경·재설정 또는 관리자 조치 후 기존 session identifier를 더 이상 인증에 사용할 수 없게 만드는 작업이다.

## CSRF 계약

Cookie 기반 인증에서 외부 사이트가 사용자의 권한으로 상태 변경 요청을 보내지 못하도록 React client와 Spring Security가 token을 교환·검증하는 규칙이다.

## GuestPrincipal

로그인 계정이 아닌 비회원 브라우저 흐름을 연속적으로 다루기 위한 임시 주체다. 실제 인물의 신원이나 콘텐츠 소유권 자체를 증명하지 않는다.

## 콘텐츠 관리 자격

비회원 publication 또는 comment의 수정·삭제 권한을 증명하는 비밀번호 기반 자격이다. 원문은 저장하지 않고 콘텐츠 범위의 hash와 실패 상태만 관리한다.

## AbuseSignal

IP·fingerprint의 HMAC, guest identifier, 요청 패턴과 위반 기록처럼 스팸·악용 위험을 판정하는 신호다. 인증 신원이나 단독 authorization 근거로 사용하지 않는다.

## Step-up Challenge

위험도가 높은 비회원 작업에서 기본 관리 자격 외에 추가로 요구하는 scope·만료·일회성 검증이다.

## Bounded Context

특정 비즈니스 용어, 규칙과 데이터 의미가 일관되게 적용되는 경계다. TownPet에서는 기본적으로 하나의 Spring Modulith application module에 대응한다.

## Data Owner

특정 테이블이나 상태를 생성·변경·삭제할 권한을 가진 유일한 bounded context다. 다른 모듈은 공개 API 또는 event를 통해서만 변경을 요청한다.

## Module Map

Application module 목록, 각 책임과 데이터 소유권, 모듈 사이의 허용 의존성을 나타내는 구조 문서다.

## Module Promotion

기존 모듈 내부 기능에 독립적인 용어, 상태 머신, 변경 주기나 확장 요구가 생겼을 때 별도 bounded context로 분리하는 결정이다.

## Contract-first API

Controller의 request/response DTO와 frontend API client가 HTTP 입력·출력·오류를 직접 정의하고, 별도 계약 파일이나 생성 transport를 두지 않는 방식이다.

## ProblemDetail

HTTP 오류의 type, title, status, detail, instance와 TownPet application error code·field error를 일관되게 전달하는 표준 기반 응답이다.

## Cursor Pagination

마지막으로 본 항목의 안정적인 정렬 key를 cursor로 전달해 다음 목록을 조회하는 방식이다. 삽입이 많은 피드에서 offset 중복·누락을 줄인다.

## Idempotency-Key

동일 command의 네트워크 재시도가 중복 생성이나 중복 상태 변경을 만들지 않도록 요청 결과를 식별하는 client 제공 key다.

## Optimistic Concurrency

읽은 시점의 version 또는 ETag가 변경되지 않았을 때만 수정을 허용하고 충돌 시 명시적 오류를 반환하는 동시성 제어 방식이다.

## Event Publication Registry

업무 transaction에서 발행한 event와 listener별 처리 상태를 PostgreSQL에 기록해 실패·재시작 후에도 전달을 추적하고 재제출하는 저장소다.

## At-least-once Delivery

event가 유실되지 않도록 한 번 이상 전달할 수 있지만 장애·재시도로 같은 event가 중복 전달될 수 있는 보장 수준이다.

## 멱등 Listener

같은 event를 여러 번 처리해도 중복 알림, 중복 집계 또는 잘못된 상태 전이를 만들지 않는 event consumer다.

## Event Schema Version

직렬화된 event payload의 구조 버전이다. 배포·재시도 시 생산자와 consumer 코드가 달라도 호환 전략을 적용할 수 있게 한다.

## Event Backlog

아직 완료되지 않은 event publication의 수와 대기 시간이다. 처리 장애와 용량 부족을 판단하는 운영 지표다.

## UploadAsset

Object storage의 파일과 PostgreSQL의 소유자, 용도, checksum, 상태와 attachment 관계를 연결하는 `media` 모듈 aggregate다.

## Presigned Upload URL

정해진 object key, method와 짧은 만료 시간에만 유효해 브라우저가 application server를 통하지 않고 object storage에 업로드할 수 있는 서명 URL이다.

## Upload Finalize

브라우저 업로드 후 Spring이 실제 object metadata와 정책을 검증하고 `UploadAsset`을 연결 가능한 상태로 전이하는 command다.

## Object Key

Storage provider 내부에서 object를 식별하는 변경되지 않는 key다. 외부 public URL과 분리해 provider·CDN 변경에도 영속 참조를 유지한다.

## Orphan Asset

업로드되었지만 publication·profile 같은 업무 객체에 정상적으로 연결되지 않은 파일이다. 수명주기와 cleanup 정책으로 탐지·정리한다.

## SearchDocument

여러 domain aggregate의 공개 검색 필드를 publication 단위로 평탄화하고 검색 index와 filter column을 가진 `discovery` 모듈의 PostgreSQL read model이다.

## 검색 Corpus

검색어, 데이터 fixture와 기대 결과·순위를 함께 고정해 normalization, ranking, visibility와 회귀를 검증하는 사례 모음이다.

## Search Freshness

원본 aggregate 변경이 SearchDocument와 검색 결과에 반영되기까지 걸리는 시간이다.

## Reindex

원본 공개 데이터를 기준으로 SearchDocument 전체 또는 일부를 다시 생성하고 건수·version·결과를 대사하는 복구 작업이다.

## Shadow Index

현재 검색 결과에 영향을 주지 않는 새 search table/index를 병렬 생성해 품질과 데이터 정합성을 비교한 뒤 전환하는 방식이다.

## FeedDocument

피드 카드 한 장을 추가 aggregate 조회 없이 렌더링하도록 publication과 공개 subtype·집계 정보를 평탄화한 `discovery` 모듈의 PostgreSQL read model이다.

## 후보 조회

개인화 ranking 전에 공개 상태, scope, 게시판과 최신성 같은 저비용 조건으로 제한된 publication 집합을 선택하는 단계다.

## Versioned Ranking

점수 공식과 feature weight에 version을 부여해 배포 전후 결과, cursor와 품질 변화를 재현할 수 있는 정렬 방식이다.

## Stable Cursor

score·시간·고유 id 같은 완전한 정렬 key와 필요 시 ranking version을 담아 다음 페이지의 중복·누락을 줄이는 cursor다.

## Stale Budget

Projection이나 cache가 원장보다 늦어도 허용되는 최대 시간이다. visibility·제재처럼 지연을 허용하지 않는 필드는 별도 안전 경로를 사용한다.

## EngagementSummary

Comment·Reaction 원장과 같은 transaction에서 atomic delta로 갱신되어 상세 화면에 정확한 집계값을 제공하는 `engagement` 모듈의 파생 상태다.

## Lost Update

여러 transaction이 같은 이전 값을 읽고 각각 저장하면서 먼저 반영된 변경이 사라지는 동시성 오류다.

## Reconciliation

원장 행에서 다시 계산한 결과와 summary·projection을 비교해 drift를 탐지하고 제한된 절차로 복구하는 작업이다.

## View Bucket

같은 viewer의 반복 조회를 일정 시간 범위에서 하나로 취급하기 위한 privacy-preserving deduplication 단위다.

## Atomic Delta

현재 값을 application에서 읽지 않고 database가 `value = value + delta`로 한 statement에서 변경해 lost update를 피하는 방식이다.

## PublicationLifecycle

작성 주체가 소유하는 게시·삭제 수명주기다. 모더레이션 제한과 분리해 작성자 삭제가 관리자 제한 해제로 되살아나지 않게 한다.

## VisibilityRestriction

특정 출처와 사유가 publication 또는 다른 대상의 노출·상호작용을 제한하는 독립 레코드다. 여러 제한이 동시에 활성화될 수 있다.

## Effective Visibility

Publication lifecycle, 활성 restriction, viewer relationship과 권한을 모두 평가한 최종 노출 결과다.

## Append-only Audit

기존 기록을 수정·삭제하지 않고 새 action과 전후 상태를 추가해 변경 이력을 보존하는 감사 방식이다.

## Legal Hold

사용자 화면에서는 비노출 또는 삭제 상태여도 법률·분쟁·운영상 정해진 기간 동안 물리 데이터를 보존하는 제한이다.

## 동등성 Matrix

기존과 신규 TownPet의 page, API, 데이터, 권한, 상태, 반응형, 접근성, SEO, 운영과 성능 기대값을 행별로 연결하고 검증 상태를 추적하는 표다.

## Differential Test

같은 논리 입력과 fixture를 legacy와 Spring 시스템에 적용하고 비결정값을 normalize한 뒤 의미 결과의 차이를 검사하는 테스트다.

## Visual Regression

고정 viewport와 데이터에서 화면 snapshot을 비교해 의도하지 않은 layout·style·content 변화를 발견하는 검증이다.

## Baseline Commit

기능 동등성 비교 대상을 고정한 기존 TownPet commit이다. 이번 재아키텍처의 최초 기준선은 `7d8f6d0bd22dedd82350c05142823ab2d101574d`다.

## 정상 월 운영비

일회성 credit이나 기간 제한 trial 없이 production을 한 달 유지할 때 반복 발생하는 compute, storage, network와 외부 service 비용이다.

## 단일 장애 지점

한 구성 요소의 실패만으로 전체 또는 핵심 기능이 중단되는 지점이다. 저비용 단일 서버 구성에서는 제거보다 탐지·backup·복구 시간으로 위험을 관리할 수 있다.

## Offsite Backup

Application·database server 장애와 disk 손실에서도 복구할 수 있도록 다른 storage 또는 provider에 보관하는 암호화된 backup이다.

## 비용 상한

정상 월 운영비가 넘지 않아야 하는 한도다. TownPet Spring production의 초기 상한은 월 1만 원이다.

## Immutable Image Digest

Tag가 아니라 container image content hash로 release를 식별해 같은 배포와 rollback에서 정확히 동일한 binary를 사용하는 방식이다.

## A/B Container Deployment

단일 VPS에서 기존 application container를 유지한 채 신규 container를 다른 내부 port에 기동·검증하고 reverse proxy upstream을 전환하는 배포 방식이다.

## Controlled Load Test

Shared CPU와 장거리 network 변동을 분리하기 위해 고정 자원·데이터·요청 조건에서 반복하는 성능 검증이다.

## Infrastructure as Code

Server, network, firewall와 bootstrap 설정을 Terraform·cloud-init·Ansible 같은 versioned 선언으로 재현하는 운영 방식이다.

## RPO

복구 시 허용하는 최대 데이터 손실 시간이다. TownPet production의 목표 Recovery Point Objective는 5분이다.

## RTO

장애 인지 후 핵심 서비스를 다시 제공하기까지 허용하는 최대 시간이다. TownPet production의 목표 Recovery Time Objective는 60분이다.

## WAL Archive

PostgreSQL 변경 기록인 Write-Ahead Log segment를 외부 저장소에 연속 보관해 base backup 이후 특정 시점까지 재생할 수 있게 하는 방식이다.

## Point-in-time Recovery

Physical base backup과 WAL을 이용해 장애 또는 실수 직전의 지정 시점으로 PostgreSQL을 복구하는 절차다.

## Restore Drill

Backup file 존재 확인이 아니라 빈 PostgreSQL 또는 새 server에 실제 복원하고 schema·query·application 동작과 소요 시간을 검증하는 훈련이다.

## Telemetry

시스템 동작을 설명하기 위해 수집하는 metric, structured log와 distributed trace의 총칭이다.

## Structured Log

사람이 읽는 자유 문장만 남기지 않고 timestamp, level, event, trace ID와 안전하게 정제된 context를 일정한 JSON field로 기록하는 log다.

## Cardinality

Metric label 값 조합의 개수다. 사용자 ID·publication ID·원문 URL처럼 값 종류가 계속 늘어나는 label은 시계열 수와 비용을 폭증시키므로 metric에 사용하지 않는다.

## Trace Sampling

모든 요청 trace를 저장하지 않고 정해진 정책으로 일부를 선택하되 오류·고지연·핵심 흐름을 우선 보존하는 방식이다.

## Telemetry Budget

Metric series, log·trace 수집량, local buffer와 외부 저장소 quota에 허용한 자원·비용 한도다.

## SLI

가용성, latency, event freshness처럼 사용자가 받은 서비스 품질을 실제 측정하는 Service Level Indicator다.

## SLO

정해진 기간 동안 SLI가 충족해야 하는 내부 품질 목표인 Service Level Objective다.

## Error Budget

SLO를 위반하지 않고 허용할 수 있는 실패 또는 저품질의 양이다. 소진 속도에 따라 배포와 안정화 작업의 우선순위를 조정한다.

## Burn Rate

Error budget이 시간에 따라 소진되는 속도다. 짧은 시간의 급격한 소진과 장기간의 완만한 소진을 서로 다른 window로 탐지한다.

## Synthetic Probe

정해진 위치에서 실제 사용자 요청과 유사한 동작을 주기적으로 실행해 가용성·latency를 능동 측정하는 검사다.

## Authenticated Canary

최소 권한 전용 계정으로 로그인과 권한이 필요한 핵심 여정을 주기적으로 검증하는 production 합성 사용자다.

## Server Timing

장거리 network와 browser rendering을 제외하고 application·database 등 server 내부 처리에 소비된 시간을 나타내는 측정값이다.

## 기술 기준선

프로젝트가 공식적으로 지원하고 CI·production에서 강제하는 Java, framework, build tool과 핵심 library의 세대다.

## Java Toolchain

개발자의 system JDK와 무관하게 Gradle compile·test가 지정한 Java language·runtime version을 사용하도록 선택·검증하는 기능이다.

## Preview Feature

Java 표준에 최종 확정되지 않아 release마다 변경·삭제될 수 있고 compile·runtime에 별도 opt-in flag가 필요한 기능이다.

## Release Provenance

배포 artifact를 만든 source commit, dependency, toolchain, build workflow와 image digest를 추적할 수 있는 증거다.

## Write Model

Aggregate invariant와 상태 전이를 transaction 안에서 검증하고 원장 데이터를 변경하는 명령 측 model이다.

## CQRS-lite

별도 서비스·database로 완전히 분리하지 않으면서 한 application과 PostgreSQL 안에서 write model과 read query의 책임·도구를 구분하는 방식이다.

## Schema Authority

Table, column, constraint, index와 extension의 의도된 상태를 정의하는 유일한 변경 체계다. TownPet에서는 Flyway migration이 이 역할을 맡는다.

## OSIV

Web request가 끝날 때까지 persistence context를 열어 controller·view에서 lazy loading을 허용하는 Open Session in View 패턴이다. TownPet에서는 query와 transaction 경계를 명확히 하기 위해 비활성화한다.

## Query-count Test

한 use case가 실행한 SQL statement 수를 검증해 데이터 증가와 함께 숨어 나타나는 N+1 query를 자동 탐지하는 integration test다.

## UUIDv7

Millisecond timestamp와 random bit를 결합한 RFC 9562 UUID로, 분산 생성의 장점을 유지하면서 시간순 B-tree insertion locality를 개선한다.

## Canonical UUID

UUID를 8-4-4-4-12 형태의 lowercase hexadecimal과 hyphen으로 표현한 표준 문자열 형식이다.

## Optimistic Lock

읽은 version과 갱신 시점 version이 같은지 비교해 동시 변경을 탐지하고 silent overwrite 대신 충돌로 처리하는 방식이다.

## Conditional Update

`UPDATE ... WHERE`에 현재 상태·잔여 수량 같은 조건을 넣고 영향받은 row 수로 성공을 판단해 한 statement에서 경합 invariant를 지키는 방식이다.

## Expand/Contract Migration

새 schema를 이전 application과 호환되게 먼저 추가하고 data backfill·code 전환 후 더 이상 쓰지 않는 schema를 나중에 제거하는 무중단 변경 방식이다.

## Forward-fix

이미 production에 적용된 migration을 되감아 history를 바꾸기보다 새로운 migration과 release로 문제를 수정하는 복구 원칙이다.

## RBAC

Actor에게 부여된 역할에 따라 큰 범주의 접근 권한을 결정하는 Role-Based Access Control이다.

## Resource Attribute Policy

Role뿐 아니라 소유자, aggregate 상태, 공개 범위, 제재와 관계 같은 대상 속성을 평가해 action 허용 여부를 결정하는 정책이다.

## Deny by Default

명시적으로 허용된 actor·action·resource 조합이 아니면 접근을 거부하는 보안 원칙이다.

## IDOR

요청의 resource identifier를 다른 값으로 바꿨을 때 서버가 object-level 권한을 확인하지 않아 타인의 데이터를 읽거나 변경하게 되는 취약점이다.

## Assurance

현재 session의 인증 방식, MFA·재인증 여부와 경과 시간으로 표현하는 신원 확인 강도다.

## Impersonation

운영자나 지원 담당자가 실제 사용자인 것처럼 application session과 권한을 사용하는 기능이다. TownPet에서는 별도 승인 설계 없이는 금지한다.

## Dual Control

한 명의 관리자 판단만으로 큰 피해를 만들 수 있는 작업에 두 번째 독립 승인 또는 분리된 권한을 요구하는 통제다.

## Showcase Production

공개 URL과 실제 운영 infrastructure를 사용하지만 실제 community 회원을 모집하지 않고 합성 데이터와 제한된 demo actor로 포트폴리오 기능을 증명하는 환경이다.

## Demo Account

실제 개인을 식별하지 않고 정해진 역할·상태·시나리오를 재현하기 위해 seed되는 공개 체험용 계정이다.

## Scenario Namespace

동시에 체험하는 사용자의 write가 서로 덮어쓰거나 scenario를 망가뜨리지 않도록 demo data를 논리적으로 격리하는 범위다.

## Launch Readiness

Showcase가 아닌 실제 사용자 서비스를 열기 전에 개인정보, 약관, 국외 이전, moderation, 보안과 운영 준비가 충족됐는지 확인하는 승인 조건이다.

## Seed Manifest

Demo 계정, 기준 resource, 관계와 기대 checksum을 version control해 showcase 상태를 재현·검증하는 선언이다.

## Scoped Reset

Database 전체를 초기화하지 않고 확인된 demo actor와 그 파생 데이터만 idempotent하게 기준 상태로 복구하는 maintenance 작업이다.

## Classified Listing

Platform이 거래 계약·결제 당사자가 되지 않고 판매·대여·나눔 정보와 거래 가능 상태를 게시하는 형태다.

## Listing Lifecycle

MarketListing의 거래 가능, 예약, 완료와 취소 상태 및 허용된 전이를 표현하는 수명주기다. Publication의 게시·삭제·모더레이션 노출 상태와는 별개다.

## Terminal State

정상적인 actor command로 다른 상태로 전이할 수 없는 완료·취소 같은 종결 상태다.

## Repair Command

일반 business rule로 복구할 수 없는 데이터 오류를 사유, 권한, 전후 값과 감사 기록을 남기며 제한적으로 수정하는 운영 명령이다.

## Anti-corruption Mapping

Legacy 또는 외부 system의 용어·상태를 domain 내부 의미로 변환해 외부 model이 새 domain model을 오염시키지 않게 하는 경계다.

## Sealed Value Object

허용된 구현 종류를 compiler가 제한하고 각 구현이 자기 불변식을 생성 시점에 만족하게 하는 Java value object 계층이다.

## Discriminated Union

`listingType` 같은 구분 field에 따라 필수·금지 field와 구체 schema가 결정되는 API type이다.

## Quarantine

의미를 안전하게 자동 변환할 수 없는 legacy row를 정상 migration 대상과 분리해 공개되지 않게 보존하고 수동 판정을 기다리는 상태다.

## Hard Block

High-confidence 금지 규칙이 입력을 거부하고 수정 가능한 reason code를 반환하는 사전 정책 결정이다.

## Soft Signal

단독으로 게시·계정을 제한하지 않고 경고, 위험 점수, moderator review와 운영 측정에 사용하는 불확실한 징후다.

## Shadow Evaluation

새 정책을 실제 차단에는 사용하지 않은 채 기존 입력에 적용해 예상 판정 변화와 오탐을 측정하는 검증 방식이다.

## Moderation Corpus

정상, 위반, 우회와 과거 오탐 문장을 기대 판정과 함께 version control해 정책 회귀를 검사하는 데이터 모음이다.

## Care Coordination

TownPet이 고용·결제·보험 당사자가 되지 않고 돌봄 요청, 지원자 선택, 진행 상태와 안전 feedback을 기록하는 이웃 간 조율 workflow다.

## Reference Reward

돌봄 요청자가 게시한 참고 금액으로, TownPet 안의 결제·정산 상태나 지급 보증을 의미하지 않는다.

## Care Participant

특정 CareRequest의 요청자 또는 수락된 CareApplication의 돌봄 제공자다. 단순 지원자는 완료 feedback 등 참여자 전용 권한을 갖지 않는다.

## CareAssignment

CareApplication 수락으로 생성되어 요청자와 한 명의 돌봄 제공자 사이의 매칭, 진행, 완료·취소·중단을 소유하는 aggregate다.

## Effective Care Status

CareRequest의 모집 상태와 active 또는 terminal CareAssignment 상태를 조합해 기존 화면에 표시하는 단일 상태다.

## SightingReport

특정 LostFoundAlert에 대해 reporter, 목격 위치·시간·설명·media와 공개 범위를 소유하는 구조화된 목격 제보 aggregate다.

## Owner-only Sighting

공개 화면에는 제보 존재와 안전한 안내만 보이고 정확한 위치·사진·민감 설명은 alert owner와 제한된 reviewer에게만 공개되는 목격 제보다.

## Public Approximate Location

반경 검색과 공개 안내에 사용할 수 있도록 정확도를 낮춘 지점과 안전한 landmark label이다. TownPet의 초기 공간 정밀도는 약 250m다.

## Exact Location Evidence

목격 판단을 위해 제한적으로 보관하는 정확 좌표·상세 위치로, 암호화하고 공개 search·projection에서 제외한다.

## PostGIS Geography

지구 곡률과 meter 단위 거리 계산을 지원하는 PostGIS 공간 type이다. TownPet은 WGS84 point와 GiST index를 사용한다.

## Precision Reduction

정확 좌표를 공개하지 않기 위해 정해진 격자·정밀도로 위치를 낮춰 approximate point를 만드는 처리다.

## Resolution Outcome

LostFoundAlert가 실제로 보호자와 재회했는지, 안전하게 발견됐는지, 보호소로 인계됐는지처럼 `RESOLVED`의 확인된 결과다.

## Close Reason

해결 결과를 확인하지 못했지만 검색 종료, 중복, 잘못된 정보 또는 작성자 요청으로 제보 접수를 끝내는 구조화된 사유다.

## Last-confirmed Age

작성자가 Alert가 여전히 유효함을 마지막으로 확인한 뒤 지난 시간으로, 자동 종료 대신 reminder와 feed freshness에 사용한다.

## Quality Gate

Formatting, architecture, test, migration, contract, security와 성능 조건을 통과하지 못한 변경의 병합·release·배포를 막는 자동 검증 단계다.

## Mutation Test

Production code의 조건·연산을 의도적으로 변형하고 test가 실패하는지 확인해 단순 실행 coverage가 아니라 assertion의 결함 탐지력을 측정하는 검사다.

## Differential Coverage

Repository 전체가 아니라 pull request에서 추가·변경된 line과 branch가 test로 검증되는 비율이다.

## SBOM

Release artifact에 포함된 component와 version을 추적하는 Software Bill of Materials다.

## Flaky Test

Code 변경 없이 같은 조건에서 성공과 실패가 바뀌는 비결정적 test다. TownPet에서는 재실행 성공으로 숨기지 않고 별도 결함으로 관리한다.
