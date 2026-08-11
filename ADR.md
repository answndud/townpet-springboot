# ADR.md

## 문서 사용법

- 번호가 더 큰 accepted ADR이 앞선 ADR의 미결 사항을 구체화하거나 대체할 수 있다.
- 각 ADR의 `Open Questions`는 해당 결정을 작성한 시점의 기록이며, 이후 accepted ADR과 충돌하면 이후 결정을 따른다.
- 현재 제품 범위와 통합 기술 설계는 각각 [`docs/PRD.md`](docs/PRD.md), [`docs/TRD.md`](docs/TRD.md)에서 확인하고 실행 순서는 [`PLAN.md`](PLAN.md)를 따른다.
- 이미 accepted인 결정을 바꾸려면 기존 기록을 지우지 말고 새 ADR에 대안·영향·migration 경로를 남긴다.

## ADR-0001 - 제품 동등성을 유지하고 내부 구현은 Spring 방식으로 재설계한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet은 공개·회원·관리자 화면, 55개 API route, 구조화 게시판, 인증, 비회원 안전장치, 모더레이션, 알림, 검색, 운영 자동화를 포함한다. `townpet-springboot`의 목표는 일부 핵심 기능만 옮긴 별도 제품이 아니라, 사용자가 기존 TownPet과 다른 이질감을 느끼지 않는 완성도 높은 Spring Boot 재아키텍처다.

기존 Next.js의 route/service/query 구조를 Java로 줄 단위 번역하면 프레임워크의 장점을 살리지 못하고 기존 결합까지 복제할 수 있다. 반대로 화면과 동작을 자유롭게 바꾸면 백엔드 교체 프로젝트가 아니라 별도 제품 개발이 된다.

### Decision

`/Users/alex/project/townpet`의 현재 제품 동작을 기능 동등성 기준선으로 삼는다.

- 공개 화면, 회원 화면, 관리자 화면에서 관찰되는 URL, 주요 사용자 흐름, 권한, 상태 전이, 오류·빈 상태는 원칙적으로 유지한다.
- 기존 디자인 시스템, UI 컴포넌트, 카피, 정적 자산은 재사용할 수 있다.
- Next.js Route Handler, Prisma, NextAuth와 서버 정책 코드는 이식 대상이 아니라 요구사항과 회귀 기준으로 취급한다.
- 백엔드는 Spring Boot의 트랜잭션, Spring Security, JPA, Validation, event/outbox, 관측성 방식에 맞게 새로 설계한다.
- 의도적으로 변경하는 제품 동작이나 호환성은 별도 ADR에 이유와 영향을 기록한다.
- 기존 Next.js에서 얻은 테스트·성능 결과는 참고 기준이며 Spring 구현의 완료 증거로 재사용하지 않는다.

### Alternatives

- 추론: 핵심 기능만 선별해 Spring으로 구현하면 빠르지만 전체 TownPet 재아키텍처라는 목표와 맞지 않는다.
- 추론: 기존 서버 코드를 계층별로 일대일 번역하면 초기 추적은 쉽지만 Spring 고유의 경계와 데이터 일관성 모델을 충분히 활용하기 어렵다.
- 추론: 제품과 서버를 동시에 전면 재설계하면 개선 여지는 크지만 기능 동등성 검증 기준이 사라진다.

### Consequences

- 화면이 같아 보여도 모든 사용자·운영 흐름에 명시적인 동등성 계약과 회귀 검증이 필요하다.
- 기존 모델을 그대로 복사하지 않고 각 도메인의 불변식, 상태 전이, 데이터 소유권을 다시 정의해야 한다.
- 프런트엔드 재사용과 백엔드 교체를 분리할 수 있어 변경 원인과 회귀를 추적하기 쉬워진다.
- 기술 부채를 제거할 수 있지만, 의도적인 차이는 문서와 테스트 없이 도입할 수 없다.
- 전체 범위는 단기 MVP보다 커지며 단계별 전환과 완료 기준이 필요하다.

### Evidence

- `/Users/alex/project/townpet/README.md`: 기존 제품 목적, 대표 화면, 기술 스택과 운영 범위를 설명한다.
- `/Users/alex/project/townpet/AGENTS.md`: 기존 validation, query, service, route/UI 책임 경계와 도메인별 코드 지도를 정의한다.
- `/Users/alex/project/townpet/app/prisma/schema.prisma`: 인증, 콘텐츠, 구조화 게시판, 모더레이션, 알림, 운영 모델의 현재 데이터 구조를 보여준다.
- `/Users/alex/project/townpet/app/src/app/api`: 현재 55개 API route의 외부 동작 기준선이다.
- 사용자 지시(2026-08-10): TownPet과 다른 이질감을 느끼지 않으면서 공고의 요구 역량을 제대로 보여주는 완성도 높은 프로젝트를 요구했다.

### Open Questions

- 기준선 커밋과 이후 원본 TownPet 변경분을 어떤 방식으로 동기화할지 결정해야 한다.
- 프런트엔드와 Spring 백엔드의 저장소·배포 토폴로지를 결정해야 한다.
- 기존 운영 데이터까지 이전할지, 스키마와 시드 데이터만 재구축할지 결정해야 한다.

## ADR-0002 - 도메인별 점진 전환으로 Spring 백엔드를 교체한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet은 UI와 Next.js 서버 컴포넌트, Route Handler, Prisma query/service가 여러 경로에서 연결되어 있다. 전체 백엔드를 한 번에 교체하면 긴 기간 동안 실행 가능한 통합 상태를 잃고, 화면 회귀와 서버 회귀의 원인을 분리하기 어렵다. 반면 UI를 새로 만들면 제품 동등성을 보장하기 어렵다.

### Decision

기존 TownPet을 비교 기준으로 유지하면서 새 저장소에서 도메인별 점진 전환(Strangler Migration)을 수행한다.

- 기존 디자인, 화면 컴포넌트, 카피와 정적 자산을 새 프런트엔드의 출발점으로 사용한다.
- 프런트엔드와 서버 구현 사이에 타입이 명시된 HTTP API client 경계를 둔다.
- 인증, 게시글, 댓글·반응, 검색, 모더레이션처럼 도메인 단위로 `DB schema -> Spring application/domain -> REST API -> frontend -> contract/E2E`를 완성하고 다음 도메인으로 이동한다.
- 개발 중 아직 전환하지 않은 도메인만 임시 legacy adapter를 사용할 수 있다.
- 도메인 전환 완료 조건에는 legacy import, Prisma 접근, Next.js Route Handler와 임시 feature flag 제거가 포함된다.
- 전체 완료 시 새 제품의 모든 백엔드 요청은 Spring Boot가 처리하며, 기존 배포본은 비교·롤백 기준에서 해제한다.

### Alternatives

- 추론: 빅뱅 재작성은 최종 구조를 바로 만들 수 있지만 장기간 통합 검증과 제품 동등성 비교가 어렵다.
- 추론: 기존 Next.js 백엔드와 Spring 백엔드를 영구 공존시키면 전환 위험은 낮지만 데이터 소유권과 장애 지점이 이중화된다.
- 추론: UI까지 함께 재작성하면 코드 정리는 쉬우나 백엔드 재아키텍처와 제품 변경을 구분할 수 없다.

### Consequences

- 각 단계에서 실행 가능한 제품과 회귀 비교 기준을 유지할 수 있다.
- 임시 adapter와 routing flag가 필요하지만 제거 조건을 각 도메인의 완료 정의로 강제해야 한다.
- 공유 테이블이나 교차 도메인 트랜잭션은 전환 순서와 데이터 소유권을 먼저 결정해야 한다.
- 같은 유스케이스를 legacy와 Spring에서 비교하는 contract test와 E2E가 필요하다.
- 전환 중 이중 쓰기는 기본적으로 금지하고, 한 데이터에 한 명의 write owner만 두어야 한다.

### Evidence

- `/Users/alex/project/townpet/app/src/app`: 현재 페이지와 Route Handler가 함께 있는 App Router 구조를 보여준다.
- `/Users/alex/project/townpet/app/src/server`: query, service, auth와 운영 로직의 현재 서버 경계를 보여준다.
- `/Users/alex/project/townpet/app/src/components`: 재사용할 UI와 제품 동작의 구현 근거다.
- 사용자 승인(2026-08-10): 도메인별 점진 전환 방식 채택에 동의했다.

### Open Questions

- 도메인 전환 순서와 공유 데이터의 최초 write owner를 정해야 한다.
- legacy와 Spring 사이에 실제 운영 데이터를 이전할지 결정해야 한다.
- 전환용 adapter를 Next.js BFF에 둘지 별도 gateway에 둘지 결정해야 한다.

## ADR-0003 - 기존 데이터를 보존하는 검증 가능한 마이그레이션을 구축한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

새 데이터베이스에 시드만 넣으면 화면은 비슷하게 만들 수 있지만 기존 TownPet의 콘텐츠, 관계, 상태 이력과 운영 의미가 보존되었다고 볼 수 없다. 전체 재아키텍처에서는 데이터 모델 변경이 기존 사용자 동작과 운영 기록을 손실시키지 않는다는 증거가 필요하다. 동시에 운영 원본과 개인정보·인증 비밀을 저장소에 포함해서는 안 된다.

### Decision

기존 TownPet 데이터의 ID, 관계와 의미를 보존하는 추출·변환·적재 및 검증 파이프라인을 정식 범위에 포함한다.

- 게시글, 댓글, 반응, 북마크, 동네, 커뮤니티, 구조화 게시판, 신고, 제재, 상태 이력, 알림과 업로드 자산의 식별자·관계·상태·시각을 보존한다.
- 새 스키마에서 정규화하거나 분리한 데이터도 원본과 의미상 역변환 가능한 매핑 규칙을 문서화한다.
- 마이그레이션은 재실행해도 중복 생성이나 상태 훼손이 없는 멱등 작업으로 만든다.
- 테이블·상태별 건수, 참조 무결성, 고아 레코드, 집계 카운터와 결정적 표본을 자동 검증한다.
- 원본 운영 데이터, 개인정보, secret과 raw dump는 Git에 저장하지 않는다.
- 세션, 인증·비밀번호 재설정·이메일 검증 토큰처럼 만료되거나 탈취 위험이 있는 데이터는 이전하지 않는다.
- 비밀번호 해시는 알고리즘·파라미터 호환성과 보안 검토를 통과한 경우에만 이전하고, 그렇지 않으면 재로그인 또는 재설정 절차를 제공한다.
- 로컬·CI·포트폴리오 데모에는 동일한 관계와 경계 사례를 재현하는 결정적 익명화 fixture를 사용한다.
- 실제 컷오버 전에 스냅샷을 이용한 dry-run과 복구 리허설을 수행한다.

### Alternatives

- 추론: 새 시드 데이터만 사용하면 구현은 단순하지만 데이터 전환 능력과 기존 제품 연속성을 입증할 수 없다.
- 추론: raw dump를 직접 복원하면 값 보존은 쉽지만 새 도메인 모델로 변환하기 어렵고 개인정보 노출 위험이 있다.
- 추론: 애플리케이션 요청 시점에 lazy migration을 수행하면 중단 시간은 줄지만 읽기 경로와 장애 복구가 복잡해진다.

### Consequences

- 스키마 설계마다 원본 필드 매핑, 손실 가능성, 역추적 방법을 함께 결정해야 한다.
- 마이그레이션 코드와 검증 리포트가 일회용 스크립트가 아니라 테스트되는 배포 자산이 된다.
- 데이터베이스 종류 변경 여부가 변환 난이도와 컷오버 전략에 직접 영향을 준다.
- 실제 사용자 인증 정보는 별도의 보안·고지·재인증 정책이 필요하다.
- 운영 데이터 없이도 CI에서 경계 사례와 실패 복구를 반복 검증할 수 있다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: 보존해야 할 모델, 관계, 식별자, enum과 집계 필드를 정의한다.
- `/Users/alex/project/townpet/app/prisma/migrations`: 현재 PostgreSQL 스키마의 변경 이력과 데이터 변환 근거다.
- `/Users/alex/project/townpet/app/prisma/seed.ts`: 데모 데이터 생성과 관계 구성의 현재 기준이다.
- 사용자 승인(2026-08-10): 기존 콘텐츠와 관계를 보존하는 데이터 마이그레이션을 프로젝트 범위에 포함하기로 했다.

### Open Questions

- 목표 데이터베이스를 PostgreSQL로 유지할지 MySQL로 변경할지 결정해야 한다.
- 인증 계정의 실제 이전 범위와 사용자 재인증 정책을 결정해야 한다.
- 컷오버 시 허용 가능한 쓰기 중단 시간과 롤백 시점을 정해야 한다.

## ADR-0004 - PostgreSQL을 단일 영속 원장으로 유지한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet은 PostgreSQL의 `citext`, 배열, JSON, `pg_trgm`, GIN과 `to_tsvector`를 사용한다. Spring Boot 전환과 동시에 MySQL로 변경하면 애플리케이션 아키텍처, 검색 의미, 스키마와 데이터베이스 동작의 변화가 한 번에 섞여 기능 동등성 회귀 원인을 분리하기 어렵다. 채용 공고의 핵심 요구는 특정 데이터베이스 이름보다 요구사항을 데이터 모델과 API로 풀고 성능과 확장성을 검증한 경험이다.

### Decision

PostgreSQL을 TownPet의 단일 영속 원장으로 유지한다.

- 회원, 콘텐츠, 모더레이션, 알림, 운영 집계와 마이그레이션 상태의 최종 진실은 PostgreSQL에 저장한다.
- 기존 PostgreSQL 전용 기능은 무조건 보존하지 않고 도메인 의미, JPA 접근성, 검색 품질과 실행 계획을 기준으로 유지·정규화·대체 여부를 결정한다.
- 기존과 같은 검색 결과가 필요한 동안 `pg_trgm`, GIN과 full-text 기능을 활용할 수 있다.
- Spring Data JPA/Hibernate를 기본 영속성 계층으로 사용하되 복잡한 읽기·검색·집계는 명시적 JPQL, native SQL 또는 별도 read repository를 허용한다.
- Redis를 도입하더라도 캐시, rate limit, 단기 coordination과 같이 재구축 가능한 보조 상태만 저장하며 영속 원장으로 사용하지 않는다.
- 데이터베이스 제품 변경은 Spring 전환 완료 후 독립된 실험과 ADR 없이는 수행하지 않는다.

### Alternatives

- MySQL 전환: 채용 공고의 사용 기술과 직접 정렬되고 이종 DB 마이그레이션 경험을 만들 수 있지만, 검색·배열·대소문자 처리와 데이터 변환 위험이 Spring 전환 회귀와 결합된다.
- 도메인별 다중 데이터베이스: 각 저장소를 최적화할 수 있지만 현재 규모에서 백업, 정합성, 관측성과 복구 부담이 과도하다.
- PostgreSQL과 새 DB 이중 쓰기: 점진 비교가 가능하지만 write owner 원칙을 깨고 불일치 복구가 어려워진다.

### Consequences

- 기존 데이터와 검색 동작을 더 직접적으로 비교하고 이전할 수 있다.
- Spring 전환의 성능 차이를 데이터베이스 제품 변경과 분리해 측정할 수 있다.
- PostgreSQL 전용 기능을 JPA entity에 무분별하게 노출하지 않도록 repository 경계를 설계해야 한다.
- MySQL 사용 경험 자체는 이 프로젝트의 핵심 증거가 아니며, PostgreSQL에서의 모델링·트랜잭션·쿼리 최적화 근거를 더 깊게 제공해야 한다.
- Redis 장애 시에도 원장 데이터가 손실되지 않고 캐시 우회·재구축이 가능해야 한다.

### Evidence

- `/Users/alex/project/townpet/docker-compose.yml`: 현재 로컬 개발 데이터베이스가 PostgreSQL 16임을 보여준다.
- `/Users/alex/project/townpet/app/prisma/schema.prisma`: `citext`, 배열, JSON과 PostgreSQL datasource 사용을 보여준다.
- `/Users/alex/project/townpet/app/prisma/migrations/20260219173000_add_search_indexes/migration.sql`: `pg_trgm`, GIN과 full-text 검색 인덱스 사용 근거다.
- `/Users/alex/project/townpet/app/prisma/migrations/20260312101500_add_post_structured_search_text/migration.sql`: 구조화 검색 텍스트와 PostgreSQL 검색 인덱스의 현재 동작을 보여준다.
- 사용자 결정(2026-08-10): 모든 영속 데이터에 PostgreSQL을 사용하기로 했다.

### Open Questions

- 기존 운영 PostgreSQL의 정확한 버전·extension과 새 환경의 호환 기준을 확인해야 한다.
- 배열·JSON 필드 중 관계형 테이블로 정규화할 대상을 도메인별로 결정해야 한다.
- Redis를 어떤 시점과 조건에서 도입할지 별도 결정해야 한다.

## ADR-0005 - Next.js와 Node.js 서버 런타임을 제거한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Next.js를 프런트엔드에 유지하면 UI 재사용은 쉽지만 Server Component, Route Handler, NextAuth와 Node.js 런타임이 남아 새 프로젝트의 백엔드 소유권이 모호해질 수 있다. `townpet-springboot`는 모든 인증, 데이터, 정책, 검색, 운영 처리를 Spring Boot가 담당하는 재아키텍처를 목표로 한다. 동시에 기존 UI와 사용자 경험은 최대한 보존해야 한다.

### Decision

새 TownPet에서 Next.js와 운영 Node.js 서버 런타임을 제거한다.

- Next.js App Router, Server Component, Route Handler, middleware, NextAuth, `next/image`, `next/link`, Next metadata API에 대한 런타임 의존을 제거한다.
- 브라우저 프런트엔드는 정적 asset으로 빌드하고 운영 환경에서는 Spring Boot 또는 그 앞단의 정적 파일 계층이 제공한다.
- 모든 서버 상태, 인증·인가, 비즈니스 정책, API, SSR이 필요한 동적 metadata와 데이터 접근은 Spring Boot가 소유한다.
- 기존 React 컴포넌트, CSS, 디자인 token, 카피와 정적 자산은 프레임워크 의존을 제거한 뒤 재사용할 수 있다.
- 개발 도구로 Node.js를 사용하는 것은 허용하지만 운영 애플리케이션의 서버 런타임으로 사용하지 않는다.
- 구체적인 브라우저 프런트엔드 기술은 별도 결정으로 확정한다.

### Alternatives

- Next.js 유지: UI와 SSR 재사용은 가장 쉽지만 두 서버 런타임과 백엔드 경계가 남는다.
- 전체 Spring MVC template 전환: 운영 런타임은 단순하지만 기존 React UI 재사용률이 낮고 상호작용 화면을 크게 재작성해야 한다.
- 별도 Node SSR 프레임워크: Next.js 종속은 줄지만 Node.js 서버와 이중 런타임 문제는 유지된다.

### Consequences

- 운영 토폴로지와 백엔드 책임이 Spring Boot 중심으로 명확해진다.
- 기존 Next.js 전용 기능과 컴포넌트는 브라우저 표준 또는 Spring 기능으로 교체해야 한다.
- 공개 페이지의 SEO와 Open Graph metadata를 유지하기 위한 Spring MVC shell 또는 별도 prerender 전략이 필요하다.
- 프런트엔드 정적 빌드는 Java 빌드와 통합하되 독립적인 lint, typecheck와 브라우저 테스트를 유지해야 한다.
- Next.js가 제공하던 이미지 최적화, route prefetch, server rendering과 캐시를 각각 측정 가능한 방식으로 대체해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/package.json`: 현재 Next.js, React, NextAuth와 프런트엔드 빌드 의존성을 보여준다.
- `/Users/alex/project/townpet/app/src/app`: 제거하거나 변환해야 할 App Router 페이지, metadata와 Route Handler의 범위다.
- `/Users/alex/project/townpet/app/src/components`: 프레임워크 의존을 제거한 뒤 재사용할 React UI의 근거다.
- 사용자 결정(2026-08-10): 새 프로젝트에서 Next.js를 제거하기로 했다.

### Open Questions

- React+Vite, Preact, Spring template+HTMX 중 브라우저 UI 구현 방식을 결정해야 한다.
- 동적 metadata와 direct URL 진입을 처리할 Spring HTML shell의 범위를 정해야 한다.
- 프런트엔드 asset을 Spring Boot jar에 포함할지 CDN에서 제공할지 결정해야 한다.

## ADR-0006 - React, TypeScript와 Vite로 브라우저 UI를 구성한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Next.js를 제거한 뒤에도 TownPet의 비테스트 TSX UI source 181개, 복합 피드, 에디터, 댓글·반응, 알림센터, 검색 자동완성과 관리자 상호작용을 보존해야 한다. Spring template과 HTMX는 운영 JavaScript를 줄일 수 있지만 기존 UI 재사용률이 낮고 풍부한 상호작용을 다시 구현해야 한다. Preact는 더 작은 번들을 만들 수 있지만 기존 React 생태계 의존성의 호환성 검증 비용이 있다.

### Decision

브라우저 프런트엔드를 React + TypeScript + Vite로 구성한다.

- Vite는 개발 서버, 정적 빌드와 테스트 도구로만 사용하며 운영 Node.js 서버를 두지 않는다.
- React Router가 기존 공개·회원·관리자 URL을 보존한다.
- 개발 환경에서 Vite는 `/api/**`를 Spring Boot로 proxy하고, 운영 환경에서는 Spring Boot와 같은 origin에서 API와 asset을 제공한다.
- 기존 React 컴포넌트는 Next.js import, Server Component, server action과 framework metadata 의존을 제거한 뒤 이관한다.
- Spring MVC가 direct URL 진입을 HTML shell로 forward하고, 공개 콘텐츠에 필요한 title·description·Open Graph metadata를 생성한다.
- Redux 같은 전역 상태 도구는 기본값으로 추가하지 않는다. 서버 상태 동기화 요구와 측정 가능한 이점이 확인될 때 별도 결정한다.
- Preact 호환 전환은 기능 동등성 완료 후 실제 번들·렌더링 측정과 라이브러리 호환성 검증을 통과한 경우에만 고려한다.

### Alternatives

- Spring template + HTMX: 운영 런타임은 단순하지만 기존 React UI 재사용률이 낮고 상호작용 화면의 재개발 범위가 크다.
- Preact + Vite: 브라우저 번들은 더 작을 수 있지만 React 전용 라이브러리와 에디터 호환 위험이 있다.
- 순수 TypeScript/Web Components: 프레임워크 의존은 줄지만 기존 컴포넌트와 테스트 자산을 대부분 재작성해야 한다.

### Consequences

- 기존 디자인과 상호작용을 가장 높은 비율로 재사용하면서 Next.js 런타임을 제거할 수 있다.
- Java와 TypeScript 두 빌드 체인을 유지하지만 운영 서버는 Spring Boot 하나로 제한된다.
- Next.js가 제공하던 SSR, image optimization, metadata, prefetch를 명시적으로 대체하고 회귀 검증해야 한다.
- 프런트엔드 빌드 산출물과 Spring resource 통합을 CI에서 재현해야 한다.
- SPA navigation, API 오류와 인증 만료를 공통 client 계층에서 일관되게 처리해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/src/components`: 이관할 React UI와 상호작용 컴포넌트의 현재 구현이다.
- `/Users/alex/project/townpet/app/src/app`: React Router로 보존할 공개·회원·관리자 URL의 기준이다.
- `/Users/alex/project/townpet/app/package.json`: React, TypeScript, SunEditor와 브라우저 테스트 의존성의 현재 근거다.
- 사용자 승인(2026-08-10): React + TypeScript + Vite를 새 프런트엔드 기반으로 채택했다.

### Open Questions

- 서버 상태 도구 없이 시작할 화면과 TanStack Query 같은 도구가 필요한 화면을 분류해야 한다.
- React asset을 실행 jar에 포함할지 별도 정적 배포 계층을 둘지 결정해야 한다.
- Spring HTML shell의 metadata와 React hydration 전 데이터 전달 계약을 설계해야 한다.

## ADR-0007 - Spring Modulith 기반 모듈형 모놀리스를 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet은 인증, 커뮤니티, 다양한 구조화 게시판, 모더레이션, 검색, 알림과 운영 기능을 포함해 단순 계층형 CRUD 애플리케이션보다 도메인 간 관계가 많다. 하나의 거대한 controller-service-repository 계층으로 구성하면 다른 도메인의 entity와 repository를 직접 참조하기 쉽고, 향후 변경·테스트·분리 비용이 커진다. 반대로 현재 트래픽과 운영 규모에서 MSA를 도입하면 분산 트랜잭션, 배포, 관측성과 장애 복구 복잡성이 근거 없이 증가한다.

### Decision

하나의 Spring Boot 배포 단위와 하나의 Gradle 애플리케이션 안에 Spring Modulith로 검증하는 도메인 모듈을 구성한다.

- 최상위 기능 패키지를 application module 경계로 사용한다.
- 모듈은 외부에 공개한 API와 이벤트만 제공하고 내부 entity, repository와 구현 타입을 다른 모듈이 직접 참조하지 못하게 한다.
- `ApplicationModules.verify()`와 모듈 단위 통합 테스트를 CI에 포함해 순환 참조, 내부 접근과 허용되지 않은 의존성을 차단한다.
- 복잡한 모듈은 `api`, `application`, `domain`, `infrastructure` 역할을 구분하되 모든 모듈에 동일한 계층 폴더를 기계적으로 강제하지 않는다.
- 즉시 일관성이 필요한 유스케이스는 동기 모듈 API와 동일 PostgreSQL 트랜잭션을 사용한다.
- 알림, 검색 색인, 분석 집계처럼 commit 이후 처리 가능한 부수효과는 application/domain event로 결합을 낮춘다.
- 초기에는 Gradle 물리 subproject를 만들지 않는다. 독립 빌드·배포·소유권 요구가 증명된 모듈만 후속 ADR을 통해 분리한다.
- MSA는 트래픽, 장애 격리, 배포 독립성 또는 조직 소유권의 실측 근거가 생기기 전에는 도입하지 않는다.

### Alternatives

- 전통적 계층형 모놀리스: 시작은 단순하지만 도메인 간 repository/entity 직접 참조를 구조적으로 차단하기 어렵다.
- 초기 Gradle 멀티모듈: 컴파일 경계는 강하지만 전체 도메인 경계가 확정되기 전에 빌드 구조와 boilerplate가 커진다.
- MSA: 독립 확장은 가능하지만 현재 범위에서 분산 데이터 일관성, 배포와 운영 부담이 과도하다.
- 순수 ArchUnit 규칙: 세밀한 규칙을 만들 수 있지만 Spring application module, 이벤트, 모듈 테스트와 문서화를 별도로 구성해야 한다.

### Consequences

- 단일 프로세스와 데이터베이스의 운영 단순성을 유지하면서 도메인 경계를 실행 가능한 규칙으로 만든다.
- 공개 모듈 API와 이벤트 schema를 신중하게 설계해야 한다.
- JPA entity 관계를 모듈 경계 밖으로 직접 연결하지 않고 식별자와 조회 projection을 활용해야 한다.
- 모듈 간 동기 호출과 이벤트 처리의 일관성 수준을 유스케이스별로 명시해야 한다.
- Spring Modulith 의존성과 구조 검증 테스트가 핵심 품질 게이트가 된다.

### Evidence

- `/Users/alex/project/townpet/app/src/server/services`: 기존 도메인 정책과 write orchestration이 이미 기능별로 일부 분리되어 있음을 보여준다.
- `/Users/alex/project/townpet/app/src/server/queries`: read model과 write service가 분리된 현재 설계 근거다.
- `/Users/alex/project/townpet/app/prisma/schema.prisma`: 하나의 애플리케이션 안에 여러 상태 머신과 도메인 관계가 공존함을 보여준다.
- Spring Modulith 공식 문서: application module의 공개·내부 경계, 순환 참조와 허용 의존성 검증, 모듈 단위 통합 테스트를 지원한다.
- 사용자 승인(2026-08-10): Spring Modulith 기반 단일 Gradle 모듈형 모놀리스를 채택했다.

### Open Questions

- 실제 bounded context와 각 테이블의 소유 모듈을 확정해야 한다.
- 모듈 간 동기 API와 event를 선택하는 일관성 기준을 유스케이스별로 정의해야 한다.
- 공통 코드가 도메인 모듈을 우회하는 공유 저장소가 되지 않도록 허용 범위를 정해야 한다.

## ADR-0008 - 공통 Publication과 도메인별 Aggregate를 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 Prisma `Post`는 공통 게시 정보와 함께 병원 후기, 장소 후기, 산책 경로, 장터, 돌봄, 입양, 봉사, 모임, 분실·목격과 Q&A 상세 모델을 1:1로 연결한다. 이 구조를 하나의 JPA aggregate 또는 상속 계층으로 옮기면 공통 게시 수명주기와 서로 다른 업무 상태 머신이 결합되고, 대부분의 subtype에 null이 많은 모델이나 거대한 서비스가 생길 수 있다. 반대로 모든 게시 유형을 완전히 독립시키면 피드, 댓글, 반응, 신고와 URL의 공통 동작이 중복된다.

### Decision

공통 게시·노출 수명주기를 소유하는 `Publication` aggregate와 구조화 게시판별 aggregate를 분리한다.

- `Publication`은 식별자, 작성 주체, 커뮤니티·동네, `LOCAL / GLOBAL`, 제목·본문, 공개·삭제 상태, 공통 시각과 필요한 집계값을 소유한다.
- 자유글처럼 추가 업무 상태가 없는 유형은 `Publication`만으로 완성한다.
- 분실·목격, 장터, 돌봄, 입양·봉사, 병원·장소 후기, 산책, 모임과 Q&A는 고유 필드와 상태 전이를 각 도메인 aggregate가 소유한다.
- 도메인 aggregate는 `Publication` JPA entity를 직접 연관 매핑하지 않고 `publicationId` 값만 보유한다. 데이터베이스 FK는 유지한다.
- 구조화 게시글 생성·수정에서 즉시 일관성이 필요한 `Publication`과 도메인 aggregate 변경은 공개 모듈 API를 통해 같은 PostgreSQL 트랜잭션에서 처리한다.
- 댓글, 반응, 북마크, 신고는 `publicationId`를 대상으로 각 소유 모듈이 처리한다.
- 피드, 검색과 관리자 목록은 aggregate graph를 순회하지 않고 전용 projection/read repository로 조회한다.
- JPA inheritance와 모든 subtype 필드를 한 테이블에 두는 nullable schema는 사용하지 않는다.

### Alternatives

- 거대한 `Post` aggregate: 공통 트랜잭션은 단순하지만 모든 도메인의 변경 이유와 연관을 한 경계에 모은다.
- JPA 상속: 객체 다형성은 제공하지만 조회·스키마·마이그레이션과 subtype별 상태 관리가 복잡해진다.
- 완전 독립 게시 모델: 도메인 자율성은 높지만 URL, 피드, 댓글, 반응, 신고와 공통 정책이 중복된다.
- JSON subtype payload: schema 변화는 쉽지만 FK, 제약, 인덱스와 데이터 대사가 약해진다.

### Consequences

- 사용자에게는 동일한 게시글이지만 서버에서는 공통 게시와 업무 상태를 독립적으로 발전시킬 수 있다.
- 구조화 게시글 유스케이스를 조정하는 application service와 명시적인 트랜잭션 경계가 필요하다.
- `publicationId` 참조의 존재·상태를 모듈 API와 DB 제약 중 어디서 확인할지 유스케이스별로 정해야 한다.
- 목록·검색 응답을 위한 조인 projection과 쓰기 aggregate 모델이 분리된다.
- 기존 `PostType` 하나에 의존한 분기를 도메인별 명령과 정책으로 분해해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: `Post` 공통 필드와 1:1 구조화 상세 모델, 댓글·반응·신고 관계의 현재 기준이다.
- `/Users/alex/project/townpet/app/src/server/services/posts/post-create-variants.ts`: 게시 유형별 생성 분기와 구조화 필드 처리의 현재 구현 근거다.
- `/Users/alex/project/townpet/app/src/server/queries/posts`: 공통 피드·상세 read model의 현재 조회 요구를 보여준다.
- 사용자 승인(2026-08-10): 공통 `Publication`과 도메인별 aggregate 분리 모델을 채택했다.

### Open Questions

- 구조화 게시판을 어떤 bounded context 묶음으로 나눌지 확정해야 한다.
- 공통 집계값을 `Publication`에 동기 저장할지 read model에서 계산할지 항목별로 결정해야 한다.
- Publication 삭제·숨김과 도메인 상태 전이의 우선순위와 복구 규칙을 정의해야 한다.

## ADR-0009 - Spring Security와 PostgreSQL 서버 세션을 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet은 NextAuth JWT session과 `sessionVersion`으로 Credentials, Kakao·Naver 로그인, 계정 연결·해제와 세션 무효화를 처리한다. 새 프런트엔드와 Spring API는 같은 origin에서 제공되고 현재 모바일 앱이나 제3자 공개 API가 없으므로 브라우저 인증에 access/refresh JWT를 도입할 필요가 작다. PostgreSQL을 단일 영속 원장으로 사용하기로 했으므로 여러 Spring 인스턴스가 공유할 수 있는 서버 세션을 구성할 수 있다.

### Decision

브라우저 인증은 Spring Security와 Spring Session JDBC의 PostgreSQL 서버 세션으로 구현한다.

- 브라우저에는 예측 불가능한 opaque session identifier를 `HttpOnly`, `Secure`, 적절한 `SameSite` cookie로만 전달한다.
- 로그인 성공 시 session fixation 방어를 적용하고 로그아웃, 비밀번호 변경·재설정, 현재 인증 수단 연결 해제와 관리자 조치에서 관련 세션을 즉시 폐기한다.
- Credentials, Kakao와 Naver 인증 수단을 하나의 회원 identity에 명시적으로 연결하며 provider email만으로 계정을 자동 병합하지 않는다.
- state-changing 요청은 Spring Security CSRF 보호를 적용하고 React API client가 정해진 CSRF 계약을 사용한다.
- 비밀번호는 검증된 adaptive hash를 사용하고 hash algorithm과 cost 변경을 지원한다.
- 로그인 rate limit, 실패·성공·세션 폐기·계정 연결 변경을 개인정보 최소화 원칙에 따라 감사 로그로 남긴다.
- 역할과 제재 상태는 인증 성공 이후 authorization 정책에서 다시 검증한다.
- Spring Session JDBC 만료 세션 정리와 session store 지표를 운영 범위에 포함한다.
- Redis session 전환은 PostgreSQL session 부하가 측정되고 저장소 교체 실험을 통과한 경우에만 별도 ADR로 결정한다.
- 모바일 앱이나 제3자 API가 생기면 기존 브라우저 세션과 분리된 OAuth2/token 경계를 설계한다.

### Alternatives

- access/refresh JWT: 별도 API client에는 유리하지만 현재 동일 origin 브라우저에서 토큰 회전, 재사용 탐지와 폐기 목록 복잡성이 추가된다.
- 애플리케이션 메모리 세션: 단순하지만 다중 인스턴스, 재시작과 강제 로그아웃 요구를 충족하지 못한다.
- 초기 Redis session: 공유 세션에는 적합하지만 PostgreSQL만으로 충분한지 측정하기 전에 운영 저장소가 추가된다.
- 기존 NextAuth JWT 형식 호환: 점진 로그인 유지에는 유리하지만 NextAuth 종속과 token verification 이중 구현을 남긴다.

### Consequences

- 세션 폐기와 사용자별 활성 세션 관리가 즉시 일관되게 동작한다.
- 상태 변경 API에 CSRF token 처리와 cookie 보안 설정이 필수다.
- PostgreSQL에 세션 read/write 부하와 만료 정리 작업이 추가되므로 지표와 부하 테스트가 필요하다.
- 기존 NextAuth JWT session은 마이그레이션하지 않으며 컷오버 시 사용자가 다시 로그인해야 한다.
- 소셜 로그인 callback과 계정 연결·해제 흐름을 Spring Security 기준으로 다시 검증해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/src/lib/auth.ts`: 현재 JWT session, Credentials, Kakao·Naver, `sessionVersion` 검증의 구현 근거다.
- `/Users/alex/project/townpet/app/src/server/services/auth/auth.service.ts`: 비밀번호 변경·재설정과 소셜 계정 연결·해제의 현재 불변식을 보여준다.
- `/Users/alex/project/townpet/app/prisma/schema.prisma`: User, Account, Session, password reset과 auth audit 데이터의 현재 구조다.
- 사용자 승인(2026-08-10): Spring Security와 PostgreSQL 서버 세션 방식을 채택했다.

### Open Questions

- 세션 cookie 이름, TTL, 동시 세션 수와 장기 로그인 정책을 결정해야 한다.
- 비밀번호 hash algorithm과 cost를 성능·보안 측정 후 결정해야 한다.
- 기존 계정의 password hash 호환성 및 재로그인 안내 정책을 확정해야 한다.

## ADR-0010 - 비회원 자격과 Abuse Signal을 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet은 비회원 글·댓글 작성과 관리 비밀번호 기반 수정·삭제를 제품 기능으로 제공한다. 기존 구현은 `GuestAuthor`, IP·fingerprint HMAC, ban, violation과 step-up nonce를 사용한다. IP와 fingerprint는 공유·변경·위조될 수 있어 작성자 소유권의 근거로 사용하기 어렵지만, abuse 탐지와 rate limit에는 유용하다. 비회원 쓰기를 제거하면 현재 제품 동작과 접근성이 크게 달라진다.

### Decision

비회원 쓰기를 유지하고 `GuestPrincipal`, 콘텐츠별 관리 자격과 `AbuseSignal`을 서로 다른 책임으로 모델링한다.

- 서버가 예측 불가능한 비회원 식별자를 안전한 cookie로 발급해 같은 브라우저의 연속성과 제한 보조 신호로 사용한다.
- 비회원이 생성한 publication 또는 comment마다 관리 비밀번호의 adaptive hash와 실패·잠금 상태를 가진 관리 자격을 연결한다.
- 수정·삭제는 관리 비밀번호를 다시 검증하며 비회원 cookie나 IP 일치만으로 허용하지 않는다.
- IP와 fingerprint 원문은 저장하지 않고 secret pepper를 사용한 HMAC 식별자를 abuse 탐지, rate limit과 기간 제재에만 사용한다.
- 다른 기기에서도 올바른 관리 비밀번호와 필요한 step-up을 통과하면 콘텐츠를 관리할 수 있다.
- 비밀번호·관리 token은 URL, query string, 응답 payload, 일반 로그와 분석 event에 포함하지 않는다.
- rate limit은 IP HMAC, guest identifier, resource와 action의 복합 신호로 적용한다.
- step-up challenge는 위험도와 반복 실패가 기준을 넘는 요청에만 적용하고 scope·만료·일회 사용을 검증한다.
- 회원 가입이나 로그인만으로 비회원 콘텐츠를 자동 귀속하지 않는다. 별도 증명·감사 가능한 claim 유스케이스가 생길 때만 추가한다.
- 제재에는 사유, 범위, 시작·만료와 해제 기록을 남기며 영구적인 기기 신원으로 간주하지 않는다.

### Alternatives

- 비회원 쓰기 제거: 보안과 운영은 단순해지지만 제품 동등성과 공개 커뮤니티 접근성을 잃는다.
- IP/fingerprint 기반 소유권: UX는 단순하지만 NAT, 기기 변경과 위조 때문에 권한 근거로 부적절하다.
- 비회원 cookie만으로 수정·삭제: 편리하지만 cookie 탈취·삭제 시 보안과 복구 문제가 있다.
- 비회원마다 일반 회원 계정 생성: 인증 모델은 통일되지만 이메일 없는 임시 계정과 개인정보 수명주기가 복잡해진다.

### Consequences

- 인증된 회원과 비회원의 authorization 경로를 명시적으로 분리해야 한다.
- 콘텐츠별 credential hash와 실패 상태가 추가되고 보안 cleanup 정책이 필요하다.
- abuse 제한은 여러 신호를 결합하지만 어느 신호도 실제 인물의 고유 식별자로 주장할 수 없다.
- 기존 guest 데이터에서 관리 자격과 abuse signal을 분리하는 마이그레이션 매핑이 필요하다.
- 비회원 관리 흐름을 다른 기기, cookie 삭제, IP 변경과 반복 실패 조건에서 테스트해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: `GuestAuthor`, `GuestBan`, `GuestViolation`, `GuestStepUpNonce`와 비회원 콘텐츠 관계의 현재 모델이다.
- `/Users/alex/project/townpet/app/src/server/services/guest-author.service.ts`: 비회원 작성자와 관리 비밀번호 처리의 현재 근거다.
- `/Users/alex/project/townpet/app/src/server/services/moderation/guest-safety.service.ts`: 비회원 abuse와 제재 정책의 현재 구현이다.
- `/Users/alex/project/townpet/app/src/server/services/posts/post-guest-management.service.ts`: 비회원 게시글 수정·삭제 권한 흐름의 현재 근거다.
- 사용자 승인(2026-08-10): 비회원 쓰기를 자격과 abuse signal이 분리된 모델로 유지하기로 했다.

### Open Questions

- 관리 비밀번호 hash를 publication·comment별로 둘지 동일 작성 흐름 단위로 공유할지 UX 기준을 확인해야 한다.
- step-up challenge 구현 수단과 접근성 기준을 결정해야 한다.
- guest cookie와 abuse 기록의 보존 기간을 개인정보 정책과 맞춰야 한다.

## ADR-0011 - 17개 Bounded Context를 기본 Application Module로 정의한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet은 공통 게시 기능 외에도 인증, 반려동물·동네 프로필, 지역 정보, 장터, 돌봄, 입양·봉사, 분실·목격, 모임, 모더레이션, 검색·피드, 알림, 업로드와 운영 지표를 포함한다. 게시 유형마다 모듈을 만들면 경계가 지나치게 잘게 나뉘고, 모든 기능을 `post`나 `service` 모듈에 넣으면 서로 다른 상태 머신과 데이터 소유권이 결합된다. Spring Modulith 검증을 적용하려면 업무 능력과 변경 이유를 기준으로 초기 module map이 필요하다.

### Decision

다음 17개 bounded context를 기본 application module로 사용한다.

- `identity`: 회원 identity, 인증 수단, 비밀번호, 역할, 세션과 인증 감사
- `member`: 프로필, 반려동물, 동네 소속과 개인 선호
- `catalog`: 동네, 커뮤니티, 품종과 게시판 기준정보
- `publication`: 공통 게시물, 작성 주체, `LOCAL / GLOBAL`, 공개 상태와 공통 URL
- `engagement`: 댓글·답변, 반응과 북마크
- `localguide`: 병원·장소·제품 후기와 산책 경로
- `marketplace`: 판매·대여·나눔과 거래 상태
- `care`: 돌봄 요청, 지원, 매칭, 진행, 완료 후기
- `welfare`: 입양 공고와 보호소 봉사 모집
- `lostfound`: 분실·목격 공고, 제보, 해결 상태와 이력
- `gathering`: 모임과 모집 상태
- `relationship`: 사용자 차단·뮤트와 노출 관계
- `trustsafety`: 신고, 제재, 비회원 abuse, 직접 모더레이션, 정정 요청과 감사 로그
- `discovery`: 피드, 검색, 자동완성, 인기글, 개인화와 조회 projection
- `notification`: 알림, 읽음 상태, delivery outbox와 재시도
- `media`: 업로드 자산, 소유권, 연결 상태와 고아 정리
- `operations`: health, Web Vitals, 획득 통계, 운영 설정과 관리 지표

추가 소유권 규칙은 다음과 같다.

- 모든 영속 테이블과 변경 가능한 상태는 정확히 한 모듈이 write owner가 된다.
- 다른 모듈은 공개 API 또는 event만 사용하고 entity·repository를 직접 참조하지 않는다.
- `discovery`가 피드·검색 read model을 소유하고 aggregate 모듈은 변경 사실과 필요한 공개 조회만 제공한다.
- `trustsafety`는 대상 데이터를 직접 갱신하지 않고 대상 모듈의 moderation API를 호출한다.
- `notification`은 다른 모듈의 transaction 내부에서 직접 insert되지 않고 완료 event를 처리한다.
- `media`만 upload asset 상태와 attachment 수명주기를 변경한다.
- 관리자 UI는 별도 도메인 모듈이 아니라 각 모듈의 운영 유스케이스를 조합하는 표현 계층으로 둔다.
- Q&A는 우선 `publication`과 `engagement`의 answer comment로 모델링하고 채택·보상 등 독립 규칙이 생기면 별도 모듈 승격을 검토한다.
- `common`에는 오류 계약, clock, correlation 같은 기술 요소만 허용하고 도메인 정책이나 공유 repository를 두지 않는다.

### Alternatives

- 게시 유형별 개별 모듈: 소유권은 선명하지만 단순 subtype까지 모듈화되어 동기 호출과 boilerplate가 증가한다.
- 모든 구조화 게시판을 `publication`에 포함: 초기 구현은 쉽지만 서로 다른 상태 머신과 변경 주기가 다시 결합된다.
- 기술 계층별 모듈: controller/service/repository 분리는 익숙하지만 업무 데이터 소유권과 변경 이유를 표현하지 못한다.
- 관리자 전용 모듈: 화면 조합은 쉬워지지만 각 도메인의 운영 정책과 데이터 변경이 중앙 관리자 서비스로 누출된다.

### Consequences

- 각 테이블, command, event와 read model의 소유자를 기능 구현 전에 명시해야 한다.
- 모듈 간 유스케이스에는 공개 identifier와 DTO 계약이 필요하다.
- 도메인 수가 많으므로 module canvas와 의존 다이어그램을 자동 생성하고 순환을 CI에서 막아야 한다.
- 실제 구현에서 함께 변경되는 불변식이 반복적으로 발견되면 ADR을 통해 module merge 또는 split을 수행할 수 있다.
- 단순 편의를 위한 공통 모듈과 cross-module JPA relation을 허용하지 않아 초기 코드가 일부 명시적으로 길어질 수 있다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: 17개 context에 배정할 현재 모델, 관계와 상태 enum의 근거다.
- `/Users/alex/project/townpet/app/src/server/services`: 인증, 게시, 모더레이션, 알림, 검색 서비스의 현재 정책 경계를 보여준다.
- `/Users/alex/project/townpet/app/src/server/queries`: 피드, 검색, 모더레이션, 알림과 운영 read model의 현재 조회 경계를 보여준다.
- `/Users/alex/project/townpet/business/제품_기술_개요.md`: `LOCAL / GLOBAL`, 구조화 게시판과 정책 우선 제품 기준의 근거다.
- 사용자 승인(2026-08-10): 제안한 17개 bounded context 지도를 기본 module 경계로 채택했다.

### Open Questions

- 각 Prisma model과 신규 table을 module별 데이터 소유권 표로 대사해야 한다.
- module 간 허용 의존성과 synchronous/event interaction matrix를 작성해야 한다.
- `localguide`와 `welfare` 내부 subtype이 독립 상태 머신으로 커질 경우 split 기준을 정해야 한다.

## ADR-0012 - [Superseded] OpenAPI 3.1 Contract-first REST API를 사용한다

- 상태: superseded by ADR-0020
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet에는 55개 Next.js Route Handler와 server action이 있고 프런트엔드가 각기 다른 입력·응답 형식을 사용한다. Spring 전환과 React 이관을 병행하면 Java DTO, HTTP 동작과 TypeScript 타입이 따로 변해 계약 drift가 발생할 수 있다. 기존 URL과 제품 동작은 유지하되 내부 API는 장기간 관리 가능한 명시적 버전과 공통 오류·페이지네이션 규칙이 필요하다.

### Decision

OpenAPI 3.1 문서를 source of truth로 사용하는 `/api/v1/**` REST API를 설계한다.

- 도메인별 수직 전환 전에 request, response, status code, 보안 요구와 오류를 OpenAPI 계약에 추가한다.
- 계약에서 Java transport interface·DTO와 TypeScript API client·type을 생성한다.
- 생성 코드는 HTTP transport 경계에만 두고 domain aggregate, JPA entity, application command와 직접 공유하지 않는다.
- Spring controller는 생성 interface를 구현하고 transport DTO를 application command/query로 명시적으로 변환한다.
- React 코드는 endpoint path와 JSON field를 수작업으로 중복 정의하지 않고 생성 client를 사용한다.
- CI에서 OpenAPI lint, code generation 재현성, controller contract test와 승인된 계약의 breaking diff를 검사한다.
- 같은 `/v1` 안에서 기존 client를 깨는 변경을 조용히 배포하지 않는다. additive change를 우선하고 필요한 breaking change는 새 major API 또는 명시적 compatibility window로 처리한다.
- 기존 Next.js API path 자체가 아니라 사용자에게 관찰되는 의미를 호환 기준으로 삼고 전환 adapter가 필요한 동안 legacy request를 `/api/v1` command로 변환한다.

공통 HTTP 규칙은 다음과 같다.

- 오류는 RFC 9457 `ProblemDetail` 형태와 안정적인 application error code, field path를 사용한다.
- 사용자 피드와 검색은 stable sort key를 가진 cursor pagination을 기본으로 한다.
- 작은 관리자 표·고정 집계는 요구가 명확할 때 offset pagination을 허용한다.
- 생성·결제성 상태 변경·재시도 가능한 command에는 scope가 명확한 `Idempotency-Key`를 사용한다.
- 충돌 가능성이 있는 수정·상태 전이는 entity version 또는 ETag/`If-Match` 기반 optimistic concurrency를 적용한다.
- 시간은 ISO-8601 UTC, 식별자는 JSON string, 금액은 정수 최소 화폐 단위로 전달한다.
- 업무 상태 전이는 임의 field patch보다 명시적 command 또는 transition resource로 표현한다.

### Alternatives

- Controller code-first 문서 생성: 구현 속도는 빠르지만 Java 구현이 계약보다 앞서고 프런트엔드 타입과의 drift를 늦게 발견할 수 있다.
- GraphQL: 다양한 read shape에는 유리하지만 현재 명령·상태 전이·HTTP cache와 운영 계약에 추가 복잡성이 생긴다.
- 기존 API shape 일대일 복제: 전환 adapter는 단순하지만 Next.js 구현 우연과 일관되지 않은 오류·페이지네이션 규칙을 영구화한다.
- 수동 Java·TypeScript DTO 유지: code generation 의존은 줄지만 두 언어의 계약 drift를 사람이 관리해야 한다.

### Consequences

- API 설계와 예시·오류까지 구현 전에 검토할 수 있다.
- code generation 설정과 생성 산출물 정책을 빌드에 포함해야 한다.
- transport DTO와 domain model 사이 mapping 코드가 생기지만 외부 계약이 내부 entity를 지배하지 않는다.
- OpenAPI가 표현하지 못하는 authorization, transaction과 상태 불변식은 별도 테스트와 문서가 필요하다.
- 기존 프런트엔드 이관 시 legacy call을 새 client로 대체하는 진행률을 추적할 수 있다.

### Evidence

- `/Users/alex/project/townpet/app/src/app/api`: 현재 55개 HTTP route의 method와 외부 동작 기준이다.
- `/Users/alex/project/townpet/business/reports/api-route-inventory.md`: 기존 API 접근 수준과 계약 inventory의 근거다.
- `/Users/alex/project/townpet/business/reports/api-route-contracts.generated.md`: 현재 validation·monitoring·test adjacency 계약 점검의 근거다.
- OpenAPI 공식 명세: 언어 독립 API 설명과 client/server code generation 사용 사례를 정의한다.
- 사용자 승인(2026-08-10): OpenAPI 3.1 contract-first REST `/api/v1` 방식을 채택했다.

### Open Questions

- OpenAPI generator와 Spring Boot 4 호환 버전을 검증해야 한다.
- 각 legacy route를 신규 operationId에 매핑하는 compatibility matrix가 필요하다.
- idempotency record 보존 기간과 응답 재생 범위를 command별로 정해야 한다.

## ADR-0020 - 별도 OpenAPI 계약 파일과 생성 client를 사용하지 않는다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: explicit

### Context

혼자 빠르게 기능을 개발하는 현재 단계에서 OpenAPI 파일, generator, 생성 client와 contract gate가 실제 runtime 기능보다 큰 변경·검증 비용을 만들었다. 현재 frontend는 얇은 수동 fetch client를 사용하고 Spring controller DTO가 이미 실제 HTTP 동작의 근거다.

### Decision

`api/openapi/townpet.yaml`, OpenAPI generator와 생성 transport를 제거한다. HTTP 계약은 Spring controller의 request/response DTO와 `frontend/src/api/client.ts`가 직접 소유한다. 별도 OpenAPI 파일을 다시 만들지 않는다.

### Consequences

- 작은 기능 변경에서 계약 파일·생성·drift 검증 비용이 사라진다.
- 계약 일관성은 controller/integration test와 frontend typecheck로 확인한다.
- 외부 소비자용 정식 API 문서가 필요해질 때는 실제 운영 요구와 새 ADR 없이는 재도입하지 않는다.

## ADR-0013 - PostgreSQL Event Publication Registry로 모듈 이벤트를 내구화한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet에서 publication 생성·수정 이후 알림, 검색·피드 projection, 통계와 운영 기록을 갱신해야 한다. 단순 in-memory Spring event는 원래 transaction commit 직후 프로세스가 종료되거나 listener가 실패하면 후속 작업을 잃을 수 있다. Kafka를 처음부터 도입하면 단일 애플리케이션 내부 이벤트를 위해 broker 운영, consumer lag, 중복·순서와 schema 관리 복잡성이 추가된다.

### Decision

Spring Modulith JDBC Event Publication Registry를 PostgreSQL에 구성해 commit 이후 모듈 이벤트 전달을 내구화한다.

- 핵심 불변식과 즉시 응답에 필요한 변경은 동기 모듈 API와 같은 PostgreSQL transaction에서 처리한다.
- 알림 생성, discovery projection, 분석 집계와 정리 가능한 부수효과는 commit 이후 module event listener가 처리한다.
- listener별 event publication을 원래 업무 transaction에 함께 저장해 업무 데이터와 전달 의도를 원자적으로 commit한다.
- event는 immutable fact로 정의하고 고유 event id, event type, schema version, aggregate identifier와 발생 시각을 포함한다.
- payload에는 필요한 identifier와 최소 변경 사실만 포함하고 JPA entity graph, secret과 불필요한 개인정보 snapshot을 직렬화하지 않는다.
- listener는 event id 또는 업무 natural key를 이용해 at-least-once 전달에서 멱등하게 동작한다.
- publication lifecycle과 completion attempt를 기록하고 실패·stale event를 지수 backoff, 최대 자동 시도와 batch 제한으로 재처리한다.
- 자동 재시도 한도를 넘은 event는 운영 큐에서 원인, 시도와 payload metadata를 확인하고 수정 후 수동 재제출할 수 있게 한다.
- 완료 event는 감사·장애 분석 보존 기간 후 archive 또는 purge해 registry table이 무한히 증가하지 않게 한다.
- 외부 broker는 application 분리, 외부 subscriber 또는 PostgreSQL 처리량 한계가 측정된 뒤 동일 event 계약을 externalize하는 방식으로 도입한다.

### Alternatives

- 단순 `@EventListener`: 구현은 쉽지만 프로세스 종료·listener 실패에서 전달 의도가 유실될 수 있다.
- 직접 outbox table·poller 구현: 제어는 세밀하지만 framework가 제공하는 publication 상태·재제출·listener 통합을 다시 만들어야 한다.
- 초기 Kafka: 높은 처리량과 외부 consumer에는 적합하지만 현재 단일 배포에서 운영 복잡성이 앞선다.
- 모든 후속 작업 동기 처리: 즉시 일관성은 강하지만 사용자 응답 지연과 일시적 외부 실패 결합이 커진다.

### Consequences

- 업무 transaction과 후속 처리 의도를 함께 보존하고 실패를 관찰·복구할 수 있다.
- event schema는 내부 구현 세부가 아니라 장기간 호환할 모듈 계약으로 관리해야 한다.
- listener 중복 실행, 순서 역전과 독성 event를 테스트해야 한다.
- event registry의 backlog, 실패율, oldest age와 재시도 횟수를 운영 지표로 수집해야 한다.
- archive·purge job과 disaster recovery 시 미완료 event 처리 절차가 필요하다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: 현재 `NotificationDelivery`와 여러 분석·운영 후속 상태의 데이터 근거다.
- `/Users/alex/project/townpet/app/src/server/services/notifications/notification.service.ts`: publication·comment 동작과 알림 생성의 현재 결합을 보여준다.
- `/Users/alex/project/townpet/app/scripts/retry-notification-deliveries.ts`: 실패 delivery 재시도 운영 요구의 현재 근거다.
- Spring Modulith 공식 이벤트 문서: 원 transaction에 listener별 publication을 기록하고 완료·실패·재제출 lifecycle을 관리하는 기능을 설명한다.
- 사용자 승인(2026-08-10): PostgreSQL Event Publication Registry와 멱등 listener·재시도 운영 큐를 채택했다.

### Open Questions

- event completion 보존 기간과 archive 정책을 정해야 한다.
- event별 ordering requirement와 병렬 처리 한도를 interaction matrix에 기록해야 한다.
- 운영 큐의 재제출 권한과 감사 로그 정책을 정의해야 한다.

## ADR-0014 - PostgreSQL Metadata와 S3 호환 Object Storage로 Media를 관리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet은 게시글, 프로필, 분실·목격 공유와 구조화 게시판에서 이미지를 사용하며 기존 운영 자산은 Vercel Blob URL과 `UploadAsset` 상태로 관리된다. Spring 인스턴스의 로컬 파일시스템은 다중 인스턴스·재배포에서 안전하지 않고, PostgreSQL에 큰 binary를 직접 저장하면 DB backup, I/O와 전송 부하가 업무 데이터와 결합된다. 클라이언트가 임의 object URL을 publication에 연결하면 소유권과 고아 파일 문제가 생긴다.

### Decision

실제 binary는 S3 호환 object storage에 두고 PostgreSQL의 `media` 모듈이 metadata, 소유권과 수명주기를 관리한다.

- 로컬·CI는 MinIO 같은 S3 호환 환경을 사용하고 운영 provider는 adapter와 configuration으로 교체 가능하게 한다.
- React는 Spring `media` API에서 권한·정책 검사를 통과한 뒤 짧은 TTL의 presigned upload URL을 받아 object storage로 직접 업로드한다.
- 업로드 전 `UploadAsset`을 `PENDING`으로 만들고 finalize 요청에서 object 존재, key, 크기, content type, signature와 checksum을 검증해 `UPLOADED`로 전이한다.
- publication·profile 등 도메인 연결 시 `media` 공개 API가 owner, 상태, 용도와 개수 정책을 확인하고 `ATTACHED`로 전이한다.
- URL이 아니라 provider와 immutable object key를 영속 식별자로 저장하고 public delivery URL은 응답 시 구성한다.
- 수명주기는 `PENDING -> UPLOADED -> ATTACHED`, 실패·위험 시 `QUARANTINED`, 만료·분리 후 `EXPIRED/DETACHED -> DELETED`를 명시한다.
- MIME 선언뿐 아니라 file signature, 크기, 이미지 pixel 수와 허용 format을 검증하고 EXIF·위치 metadata를 제거한다.
- thumbnail과 최적화 파생본은 원본 attachment 완료 event를 멱등하게 처리해 생성한다.
- 미완료·고아 asset은 보존 기간 후 정리하고 신고·법적 보존 대상은 일반 삭제보다 quarantine·retention 정책을 우선한다.
- 기존 Vercel Blob object는 migration pipeline에서 복사·checksum·건수 대사하고 원 URL에서 새 object key로 추적 가능한 mapping을 남긴다.
- presigned URL, 관리 credential과 storage secret은 일반 응답 로그와 분석 event에 남기지 않는다.

### Alternatives

- Spring local filesystem: 개발은 단순하지만 인스턴스 교체, 수평 확장과 backup에 부적합하다.
- PostgreSQL large object/bytea: 원자성은 강하지만 DB 용량·backup·I/O와 media delivery 부하가 결합된다.
- Spring을 통한 multipart proxy upload: 검증은 중앙화되지만 application bandwidth와 memory/streaming 부하가 커진다.
- provider public URL 직접 저장: 구현은 쉽지만 provider 교체, 소유권, 만료와 migration 추적이 어렵다.

### Consequences

- application server가 큰 파일 payload를 중계하지 않아도 되지만 upload request와 finalize의 2단계 흐름이 필요하다.
- DB transaction과 object storage operation은 원자적이지 않으므로 상태 머신과 cleanup으로 orphan을 복구해야 한다.
- 로컬·CI에 S3 호환 service가 추가되고 실제 provider와의 contract test가 필요하다.
- image processing, malware 검사와 quarantine listener의 실패·재시도 운영이 필요하다.
- 기존 URL 호환을 위한 redirect 또는 mapping 보존 기간을 정해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: `UploadAsset`, `PostImage`, storage provider, visibility와 lifecycle 상태의 현재 모델이다.
- `/Users/alex/project/townpet/app/src/app/api/upload`: 기존 업로드 API와 프런트엔드 계약의 근거다.
- `/Users/alex/project/townpet/app/src/server/upload.ts`: 업로드 검증과 storage 처리의 현재 구현이다.
- `/Users/alex/project/townpet/app/scripts/cleanup-upload-assets.ts`: 미완료·고아 asset 정리 요구의 현재 근거다.
- 사용자 승인(2026-08-10): PostgreSQL metadata, S3 호환 object storage와 presigned 직접 업로드를 채택했다.

### Open Questions

- 운영 object storage provider와 CDN을 배포 결정에서 확정해야 한다.
- 원본·파생본 format, 크기와 보존 정책을 화면별 성능 budget에 맞춰야 한다.
- 기존 Vercel Blob URL redirect와 object mapping 보존 기간을 정해야 한다.

## ADR-0015 - PostgreSQL SearchDocument Read Model로 검색을 제공한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet 검색은 publication 공통 필드뿐 아니라 병원, 장소, 품종, 산책과 입양 같은 구조화 필드를 함께 검색하고 `LOCAL / GLOBAL`, 게시판, 상태와 접근 정책을 적용한다. 여러 bounded context의 aggregate table을 요청마다 직접 조인하면 write model과 검색 요구가 결합되고 검색 필드·가중치·색인 성능을 독립적으로 발전시키기 어렵다. PostgreSQL은 기존에 사용하던 `pg_trgm`, GIN과 full-text 기능을 계속 제공할 수 있다.

### Decision

`discovery` 모듈이 PostgreSQL의 평탄화된 `SearchDocument` read model을 소유하고 durable module event로 갱신한다.

- 한 publication의 공개 검색 표현을 `publicationId`, 제목, 본문 text, 구조화 검색 text, 공개 작성자명, scope, community·neighborhood, type, animal·breed tag, 상태, 게시 시각과 source version으로 구성한다.
- 제목, 구조화 필드와 본문에 서로 다른 full-text 가중치를 적용하고 `tsvector`와 GIN index를 사용한다.
- `pg_trgm`은 한글 부분 일치, 자동완성과 오타 후보에 사용하고 실제 검색 corpus로 임계값과 정렬을 검증한다.
- scope, type, community, neighborhood, animal, status와 published time은 관계형 컬럼과 복합 index로 filter한다.
- source module의 create·update·visibility·delete event를 event id와 source version으로 멱등하게 반영한다.
- 숨김·삭제·제재 상태는 검색 read model에서 비노출하고 사용자 차단·뮤트 같은 viewer 정책은 결과 반환 전에 authorization filter로 적용한다.
- 검색 문서 갱신은 eventual consistency를 허용하며 backlog, oldest event와 `indexedAt` 지연을 지표로 노출한다.
- 전체 rebuild, 특정 publication repair와 shadow table 대사 command를 제공한다.
- 검색 pagination은 score, published time과 publication id를 포함한 stable cursor를 사용한다.
- 기존 검색 사례를 정답 corpus로 옮겨 exact title, 한글 부분 검색, 구조화 필드, scope, zero result, visibility와 normalization을 회귀 검증한다.
- PostgreSQL 검색이 정의한 품질·latency·색인량 기준을 넘을 때 같은 SearchDocument 계약을 이용한 Elasticsearch/OpenSearch 실험을 별도 ADR로 평가한다.

### Alternatives

- aggregate table 직접 검색: 별도 색인은 없지만 다수 도메인 join과 검색 index가 write schema에 결합된다.
- 처음부터 Elasticsearch/OpenSearch: 검색 기능은 풍부하지만 cluster 운영, 원장과의 정합성, 재색인과 비용이 앞선다.
- application memory 검색: 작은 fixture에는 단순하지만 다중 인스턴스, pagination과 데이터 증가를 처리하지 못한다.
- PostgreSQL materialized view만 사용: 조회는 단순하지만 증분 갱신·source version·개별 repair 제어가 제한된다.

### Consequences

- 쓰기 aggregate와 검색 schema를 독립적으로 최적화할 수 있다.
- 검색 결과가 짧은 시간 원본보다 늦을 수 있으므로 freshness SLO와 운영 repair가 필요하다.
- SearchDocument에 공개 가능한 최소 정보만 복제하고 개인정보·비공개 연락처를 포함하지 않아야 한다.
- event schema 변경과 전체 재색인 절차를 배포·복구 계획에 포함해야 한다.
- PostgreSQL extension, index와 query plan을 Testcontainers와 실제 운영 유사 데이터에서 검증해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/migrations/20260219173000_add_search_indexes/migration.sql`: 기존 `pg_trgm`, GIN과 full-text 검색 index의 근거다.
- `/Users/alex/project/townpet/app/prisma/migrations/20260312101500_add_post_structured_search_text/migration.sql`: 구조화 검색 text 생성과 index의 현재 구현이다.
- `/Users/alex/project/townpet/app/src/server/queries/search.queries.ts`: 검색 field, filter와 결과 shaping의 현재 요구를 보여준다.
- `/Users/alex/project/townpet/app/scripts/check-search-cases.ts`: 신규 회귀 corpus로 이전할 검색 사례의 근거다.
- 사용자 승인(2026-08-10): PostgreSQL SearchDocument, GIN/`pg_trgm`과 event 기반 색인을 채택했다.

### Open Questions

- 한글 normalization, ranking weight와 trigram threshold를 corpus 실측으로 확정해야 한다.
- search freshness SLO와 전체 rebuild 허용 시간을 정해야 한다.
- 차단·뮤트 결과를 query 단계와 후처리 중 어디서 적용할지 성능·노출 안전성 기준으로 결정해야 한다.

## ADR-0016 - PostgreSQL FeedDocument와 Versioned Ranking을 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet 피드는 `LOCAL / GLOBAL`, 게시판, 최신·인기, 반려동물·품종 개인화, 차단·뮤트와 모더레이션 상태를 결합해 카드 정보를 반환한다. 요청마다 여러 aggregate와 subtype을 조인하면 query shape와 write model이 결합되고 N+1·pagination·ranking 안정성을 통제하기 어렵다. 사용자별 완성 피드를 미리 저장하면 사용자와 publication 조합만큼 데이터가 증가하고 ranking 변경·삭제 반영 비용이 커진다.

### Decision

`discovery` 모듈이 카드 렌더링용 PostgreSQL `FeedDocument`를 소유하고 후보 조회와 versioned ranking을 분리한다.

- FeedDocument는 publication id, scope, community·neighborhood, type, 제목·요약·thumbnail, 공개 작성자 요약, 구조화 badge, 집계값, 공개 상태, 게시 시각과 source version을 가진다.
- publication, engagement와 subtype module event를 event id·source version으로 멱등 반영한다.
- 최신 피드는 `publishedAt + publicationId`, 인기 피드는 versioned score snapshot과 tie-breaker를 사용해 stable keyset pagination한다.
- 개인화 피드는 PostgreSQL에서 공개 상태·scope·게시판·최신성 기준의 제한된 후보군을 조회한 뒤 사용자 pet·breed·interest와 engagement·recency 신호로 재정렬한다.
- 사용자별 완성 feed row를 미리 무제한 materialize하지 않는다.
- ranking 공식, weight와 feature set에 version을 부여하고 cursor에 ranking version, score, published time과 id를 포함한다.
- 선호 정보가 없거나 개인화 계산이 실패하면 일반 최신 피드로 fallback한다.
- 차단, 뮤트, 제재와 숨김 정책은 응답 전에 적용하며 shared cache가 viewer별 visibility를 우회하지 못하게 한다.
- FeedDocument freshness, event backlog, query count, p50/p95와 후보·ranking phase timing을 관찰한다.
- 특정 publication repair, 전체 rebuild와 원장 대사를 지원한다.
- 캐시는 PostgreSQL query plan과 부하 측정 후에만 도입한다. 공개·비개인 응답부터 짧은 TTL과 명시적 key·무효화·stale budget을 적용하고 장애 시 PostgreSQL로 우회한다.

### Alternatives

- aggregate 실시간 join: 별도 projection은 없지만 subtype·집계·viewer 정책이 한 query에 결합되고 payload query가 복잡해진다.
- 사용자별 feed materialization: 읽기는 빠르지만 fan-out 비용과 삭제·ranking 변경 시 갱신량이 크다.
- 초기 Redis cache 중심: 빠른 응답은 만들 수 있지만 정확한 PostgreSQL query와 visibility 오류를 숨길 수 있다.
- 외부 recommendation service: 모델 독립성은 높지만 현재 신호량과 운영 규모에서 근거가 부족하다.

### Consequences

- 카드 조회와 write aggregate를 독립적으로 최적화하고 N+1을 query count test로 고정할 수 있다.
- 짧은 projection 지연이 생기므로 freshness SLO와 rebuild·repair 운영이 필요하다.
- ranking 변경은 version, cursor 호환성과 전후 품질 비교를 동반해야 한다.
- 개인화 후보 수와 계산 비용에 상한을 두고 phase별 latency를 측정해야 한다.
- cache 도입은 hit ratio뿐 아니라 stale·visibility 안전성까지 증명해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/src/server/queries/posts/post-list.queries.ts`: 피드 목록 field와 filter의 현재 조회 근거다.
- `/Users/alex/project/townpet/app/src/server/queries/posts/post-feed-personalization.queries.ts`: 개인화 후보와 선호 신호의 현재 구현이다.
- `/Users/alex/project/townpet/app/src/lib/feed-personalization-policy.ts`: ranking·fallback 정책의 현재 근거다.
- `/Users/alex/project/townpet/app/prisma/schema.prisma`: publication 집계, 사용자 선호, personalization event·stat의 현재 데이터 근거다.
- 사용자 승인(2026-08-10): PostgreSQL FeedDocument, 2단계 후보·ranking과 측정 후 cache 원칙을 채택했다.

### Open Questions

- 후보 수, feature weight, ranking version 전환과 품질 지표를 정의해야 한다.
- FeedDocument freshness와 원장 대사 주기를 정해야 한다.
- cache 도입 threshold와 viewer-independent 응답 범위를 성능 단계에서 확정해야 한다.

## ADR-0017 - Engagement 원장·동기 Summary와 View 지연 집계를 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet publication에는 댓글, 반응, 북마크와 조회수가 표시되고 피드·인기 ranking·알림에서 이 값을 사용한다. JPA entity의 현재 count를 읽어 증가시킨 뒤 저장하면 동시 요청에서 lost update가 발생할 수 있다. 모든 목록에서 원본 행을 매번 count하면 정확하지만 피드 query 비용이 커진다. 댓글·반응 수는 사용자 동작 직후 정확해야 하지만 조회수는 반복·bot·prefetch를 제외한 근사 지표로 지연 집계가 가능하다.

### Decision

`engagement` 모듈이 Comment·Reaction·Bookmark 원장과 transactionally 갱신되는 `EngagementSummary`를 소유하고 조회수는 deduplication bucket을 거쳐 지연 집계한다.

- Comment, Reaction과 Bookmark 행을 진실의 원장으로 간주한다.
- 회원당 publication reaction 하나를 database unique constraint로 보장하고 동일 command 재요청은 멱등하게 처리한다.
- LIKE·DISLIKE 전환은 하나의 application command에서 기존 상태와 summary delta를 계산해 처리한다.
- 댓글 생성, 삭제·복원과 comment count 변경은 동일 PostgreSQL transaction에서 수행한다.
- summary는 `count = count + delta` 형태의 atomic SQL과 음수 방지 check constraint를 사용하고 read-modify-write entity count 패턴을 금지한다.
- 충돌 특성과 실행 계획을 측정해 optimistic version 또는 짧은 row lock을 필요한 command에만 적용한다.
- 상세 read는 EngagementSummary를 사용하고 FeedDocument는 summary 변경 event를 eventual하게 복제한다.
- reconciliation job이 원장 행과 summary를 비교해 차이를 report하고 승인된 mode에서 repair한다.
- view는 `publicationId + privacy-preserving viewer HMAC + time bucket` unique key로 반복 조회를 제거하고 batch가 summary에 반영한다.
- raw IP를 저장하지 않고 bot, operator와 prefetch request를 view 집계에서 제외한다.
- 오래된 view deduplication row는 partition·retention 정책으로 정리하고 view freshness·drop 허용 범위를 운영 지표로 둔다.
- PostgreSQL Testcontainers 동시성 테스트로 중복 반응, 교차 전환, 댓글 생성·삭제, rollback, duplicate event와 reconciliation을 검증한다.

### Alternatives

- Publication entity에 모든 count 저장: 조회는 단순하지만 engagement write ownership과 hot row 충돌이 publication 모듈로 누출된다.
- 매 요청 `COUNT(*)`: 원장과 항상 일치하지만 피드·상세의 반복 집계 비용과 query 복잡성이 증가한다.
- 모든 count 비동기 event 처리: 처리량은 높일 수 있지만 댓글·반응 직후 사용자 화면의 정확성과 ranking 일관성이 약해진다.
- Redis counter 원장: atomic increment는 쉽지만 PostgreSQL 원장과 복구·대사 책임이 이중화된다.

### Consequences

- 정확한 engagement count와 고처리량 view count에 서로 다른 일관성 모델을 적용한다.
- summary row가 쓰기 집중 지점이 될 수 있어 lock wait와 throughput 측정이 필요하다.
- FeedDocument와 화면의 짧은 지연을 optimistic UI와 freshness 기준으로 다뤄야 한다.
- reconciliation·repair는 dry-run, 변경 건수 제한과 감사 기록을 가져야 한다.
- viewer HMAC과 view retention은 개인정보·secret rotation 정책에 포함된다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: Comment, PostReaction, CommentReaction, Bookmark와 기존 count field·unique constraint의 근거다.
- `/Users/alex/project/townpet/app/src/lib/post-reaction-score.ts`: 반응과 인기 score 계산의 현재 의미를 보여준다.
- `/Users/alex/project/townpet/app/src/app/api/posts/[id]/view/route.ts`: 기존 조회수 처리와 요청 경계의 근거다.
- `/Users/alex/project/townpet/app/scripts/repair-post-integrity.ts`: 원장과 publication 집계값 대사·repair 요구의 현재 근거다.
- 사용자 승인(2026-08-10): engagement 원장·동기 summary와 조회수 시간 버킷 지연 집계를 채택했다.

### Open Questions

- view time bucket, retention과 bot 판별 기준을 실측으로 정해야 한다.
- summary hot row의 lock·throughput budget과 shard 필요 조건을 정의해야 한다.
- reconciliation 주기와 자동 repair 허용 범위를 결정해야 한다.

## ADR-0018 - Publication 수명주기와 다중 VisibilityRestriction을 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 `PostStatus`의 `ACTIVE / HIDDEN / DELETED`처럼 단일 상태만 두면 작성자 삭제, 신고 임계치 자동 숨김, 관리자 검토, 사용자 제재와 법적 보존이 같은 값을 덮어쓸 수 있다. 한 제한을 해제하는 과정에서 다른 사유까지 실수로 해제하거나, projection 지연 중 숨겨야 할 콘텐츠가 다시 노출될 위험이 있다. 신고 처리와 실제 대상 조치도 별도 수명주기를 가진다.

### Decision

작성자가 소유하는 `PublicationLifecycle`과 여러 출처가 추가·해제할 수 있는 `VisibilityRestriction`을 분리하고 모든 관리자 조치를 append-only audit로 기록한다.

- Publication lifecycle은 게시와 작성자 삭제·복구 가능 범위를 표현하고 moderation hidden을 같은 상태 값으로 덮어쓰지 않는다.
- VisibilityRestriction은 고유 id, target, reason, source type·id, 적용 scope, 시작·만료, 해제 시각과 actor를 가진다.
- 신고 임계치 자동 숨김, moderator 검토 숨김, 작성자 제재, 법적 보존 등 여러 제한이 동시에 활성화될 수 있다.
- 각 해제 command는 자신이 대상으로 한 restriction만 종료하며 다른 활성 제한과 작성자 삭제 상태를 변경하지 않는다.
- effective visibility는 lifecycle, 모든 활성 restriction, viewer relationship과 권한을 평가해 계산한다.
- `trustsafety`는 publication table을 직접 갱신하지 않고 `publication`이 제공하는 moderation API로 restriction 적용·해제를 요청한다.
- FeedDocument·SearchDocument는 visibility event를 우선 반영하고 public 상세 API는 projection과 무관하게 원본 effective visibility를 다시 검증한다.
- Report는 `RECEIVED -> TRIAGED -> IN_REVIEW -> RESOLVED/DISMISSED` 수명주기를 가지며 접수와 실제 moderation action을 분리한다.
- 동일 reporter·target의 중복 신고를 제한하고 자동 임계치 조치는 되돌릴 수 있는 숨김까지만 허용하며 자동 영구 제재는 하지 않는다.
- 고위험 관리자 command는 idempotency key, optimistic version, actor authorization과 재인증 정책을 적용한다.
- 숨김, 복구, 제재, 해제와 실패한 시도까지 actor·사유·전후 상태를 append-only audit log에 남기고 일반 관리자 수정·삭제를 금지한다.
- 사용자 삭제 요청과 법적·운영 보존이 충돌하는 데이터는 화면 비노출과 물리 보존을 분리하고 보존 사유·기한을 기록한다.

### Alternatives

- 단일 Publication status: 조회는 단순하지만 서로 다른 제한 출처와 해제 책임을 안전하게 표현하지 못한다.
- TrustSafety의 직접 DB update: 구현은 빠르지만 publication invariant와 module data ownership을 우회한다.
- 신고 수만으로 자동 영구 제재: 운영 비용은 줄지만 coordinated abuse와 오탐에서 회복하기 어렵다.
- audit row 수정 허용: 저장량은 줄일 수 있지만 누가 무엇을 바꿨는지 신뢰 가능한 이력을 잃는다.

### Consequences

- 현재 비노출 이유를 restriction 목록으로 설명하고 특정 조치만 안전하게 되돌릴 수 있다.
- public query마다 effective visibility를 누락하지 않도록 중앙 policy와 보안 회귀 테스트가 필요하다.
- restriction 만료 처리, event 재전파와 projection 대사 job이 필요하다.
- audit와 법적 보존 데이터의 접근 권한, retention과 개인정보 정책을 별도로 관리해야 한다.
- 관리자 UI는 단일 상태 선택이 아니라 활성 제한, 출처와 해제 가능 범위를 보여줘야 한다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: `PostStatus`, Report, ReportAudit, UserSanction과 ModerationActionLog의 현재 상태·감사 모델이다.
- `/Users/alex/project/townpet/app/src/server/services/moderation/report.service.ts`: 신고 접수, 중복 확인과 조건부 자동 숨김의 현재 정책이다.
- `/Users/alex/project/townpet/app/src/server/services/moderation/sanction.service.ts`: 사용자 제재와 상호작용 차단의 현재 정책이다.
- `/Users/alex/project/townpet/app/src/server/services/moderation/direct-moderation.service.ts`: 직접 숨김·복구와 감사 기록의 현재 흐름이다.
- 사용자 승인(2026-08-10): publication lifecycle, 다중 VisibilityRestriction과 append-only audit 모델을 채택했다.

### Open Questions

- restriction reason·scope taxonomy와 우선순위를 정책 문서에서 확정해야 한다.
- 자동 숨김 threshold와 reporter trust 계산을 abuse simulation으로 검증해야 한다.
- audit·legal hold retention과 관리자 재인증 조건을 결정해야 한다.

## ADR-0019 - 동등성 Matrix와 Legacy/Spring Differential Test를 완료 기준으로 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet의 백엔드와 프런트엔드 런타임을 모두 교체하면서 사용자가 이질감을 느끼지 않게 하려면 일부 대표 E2E만으로는 부족하다. 기존에는 49개 page, 55개 API route, 비테스트 TSX UI source 181개와 다수의 정책·운영 작업이 있으며, 새로운 내부 API path와 domain model은 기존 구현과 다르다. Pixel 완전 일치는 rendering engine과 동적 값 때문에 취약하지만 주관적 수동 확인만으로도 기능·권한·오류 회귀를 놓칠 수 있다.

### Decision

기존 TownPet commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`를 제품 동작 기준선으로 고정하고 기능 동등성 matrix, 두 시스템 differential test와 visual regression을 도메인 전환 완료 조건으로 사용한다.

- 기준선에는 공개·회원·관리자 page와 API, 모델·정책, 운영 job, URL·redirect, loading·empty·error·disabled 상태를 inventory한다.
- 각 Prisma model과 field를 새 bounded context, table·column, migration rule과 검증 query에 mapping한다.
- 각 legacy route·server action을 신규 OpenAPI operationId와 사용자 흐름에 mapping한다.
- 같은 논리 fixture를 legacy와 Spring schema에 적재하고 비결정적 id·time·URL을 명시적으로 normalize한 뒤 API의 의미 결과를 비교한다.
- Playwright 사용자 시나리오를 legacy와 Spring target에 실행해 navigation, 입력, 권한, 상태 전이와 오류를 비교한다.
- 대표 page를 mobile·tablet·desktop viewport에서 visual snapshot으로 비교하되 동적 요소만 명시적으로 mask하고 의미 기반 DOM assertion을 함께 사용한다.
- keyboard·focus·label·landmark와 axe 기반 접근성, direct URL, title·description·Open Graph·canonical을 검사한다.
- migration 전후 count, FK, 상태 분포, 집계와 deterministic sample을 대사한다.
- Spring 성능은 legacy 측정과 같은 조건에서 별도로 수집하고 정한 API·browser·query budget을 통과해야 한다.
- 의도적 차이는 test skip으로 숨기지 않고 ADR, matrix expected result와 사용자 영향으로 기록한다.
- 원본 기준선 이후 변경은 검토 없이 자동 반영하지 않고 별도 baseline update 절차를 거친다.

한 도메인의 전환 완료에는 다음이 모두 필요하다.

- 기능 동등성 matrix의 해당 행 통과
- Spring unit·integration·module·concurrency test 통과
- dual-target API·E2E·visual 검증 통과
- migration dry-run과 대사 통과
- 성능·query budget 통과
- 해당 legacy adapter, feature flag, Prisma·Next.js dependency 제거
- 운영 metric, alert와 runbook 준비

### Alternatives

- 신규 시스템 테스트만 작성: 내부 정확성은 검증하지만 기존 사용자 동작과의 차이를 체계적으로 발견하지 못한다.
- 전체 pixel exact 비교: 시각 변화에는 민감하지만 font·rendering의 무해한 차이에 취약하고 정책·API 의미를 검증하지 못한다.
- 수동 QA 중심: 탐색적 검증에는 유용하지만 49개 page와 반복 회귀를 재현하기 어렵다.
- 기준선 없이 최신 legacy 추적: 원본 변경과 migration 회귀가 섞여 완료 상태를 고정할 수 없다.

### Consequences

- 기능 누락과 의도적 차이를 수치와 matrix로 추적할 수 있다.
- 두 fixture·실행 환경과 normalization layer를 유지하는 비용이 생긴다.
- visual snapshot의 mask·threshold 변경도 review 대상이 된다.
- baseline commit 이후 원본 기능은 자동 범위 확장이 아니며 별도 수용 결정이 필요하다.
- 전환 속도보다 검증 자산의 완성도가 우선되고 legacy 제거 시점이 명확해진다.

### Evidence

- `/Users/alex/project/townpet` commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`: clean `main` 상태로 확인한 제품 기준선이다.
- `/Users/alex/project/townpet/app/src/app`: 49개 page와 55개 API route inventory의 근거다.
- `/Users/alex/project/townpet/app/e2e`: 이중 target으로 전환할 기존 사용자 시나리오의 근거다.
- `/Users/alex/project/townpet/app/scripts`: API contract, 검색, 성능, 운영과 migration 검증 자산의 근거다.
- 사용자 승인(2026-08-10): 동등성 matrix, differential test와 visual regression을 공식 완료 기준으로 채택했다.

### Open Questions

- visual threshold, viewport, browser와 동적 mask 목록을 화면 inventory에서 확정해야 한다.
- legacy fixture와 신규 fixture의 논리 식별자 mapping 형식을 정해야 한다.
- baseline update 승인 절차와 신규 원본 변경의 수용 기준을 정해야 한다.

## ADR-0020 - Production 월 운영비를 1만 원 이하로 제한한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet Spring 재아키텍처는 실제 공개 deployment, PostgreSQL, object storage, backup, metric·log와 CI/CD 증거가 필요하다. 그러나 관리형 database, container service, load balancer와 observability를 각각 유료로 사용하면 개인 포트폴리오의 월 운영비가 빠르게 증가한다. 사용자는 월 운영비 상한을 1만 원으로 정했다.

### Decision

Production의 정상 월 운영비를 1만 원 이하로 제한하고 비용 제약을 architecture와 운영 완료 조건에 포함한다.

- 무료 trial·일회성 credit을 정상 비용 계산에서 제외하고 지속 가능한 공개 가격을 기준으로 한다.
- 비용 상한 안에서 sleep 없는 Spring Boot와 PostgreSQL의 기본 가용성을 우선한다.
- 관리형 서비스의 개수를 줄이고 필요하면 단일 저가 compute에서 application·PostgreSQL을 격리된 container와 resource limit으로 운영한다.
- Binary object는 application server disk의 영속 원장으로 두지 않고 S3 호환 외부 object storage를 사용한다.
- CI는 GitHub Actions, image는 공개 또는 무료 범위 registry, 관측성은 자체 최소 구성과 무료 외부 tier를 우선한다.
- Kubernetes, 유료 load balancer, 다중 AZ와 상시 standby는 production 기본 범위에서 제외한다.
- 비용 때문에 제외한 가용성 기능과 단일 장애 지점은 숨기지 않고 risk register, backup·restore와 recovery objective로 보완한다.
- 월별 실제 사용량과 예상 비용을 기록하고 상한 접근 alert와 비용 runbook을 둔다.
- provider 선택과 정확한 topology는 공식 가격·리전·자원 검증 후 별도 ADR로 확정한다.

### Alternatives

- 완전 관리형 cloud stack: 운영 부담은 낮지만 월 1만 원 상한과 양립하기 어렵다.
- 무료 sleep PaaS: 비용은 낮지만 cold start와 비결정적 가용성이 성능·운영 증거를 약화시킨다.
- 로컬 PC만 공개: 비용은 낮지만 네트워크, 전원, 보안과 재현성이 포트폴리오 운영 환경에 부적합하다.
- Kubernetes self-hosting: 학습 범위는 넓지만 한 서버에서 실질 가용성을 높이지 못하고 자원·운영 비용만 추가한다.

### Consequences

- managed PostgreSQL보다 backup, patch, disk monitoring과 restore 책임이 커질 수 있다.
- 단일 compute 장애를 허용하는 대신 복구 시간·데이터 손실 목표와 offsite backup을 실제로 검증해야 한다.
- rolling/blue-green deployment는 자원 여유와 service 특성에 맞춰 축소할 수 있다.
- 무료 외부 tier의 quota·보존 기간을 monitor하고 초과 시 degradation 정책이 필요하다.
- 비용 자체가 architecture trade-off와 운영 자동화 사례로 문서화된다.

### Evidence

- 사용자 결정(2026-08-10): TownPet Spring production의 월 운영비 상한을 1만 원 이하로 정했다.
- `/Users/alex/project/townpet/business/analytics/비용_팩트체크_및_3안_시뮬레이터.md`: 기존 TownPet의 비용 검토와 운영 대안 근거다.
- `/Users/alex/project/townpet/business/operations/장애 대응 런북.md`: 저비용 운영에서도 유지해야 할 장애·복구 기준의 근거다.

### Open Questions

- 예산 안의 compute provider, region과 architecture를 공식 가격 검증 후 선택해야 한다.
- PostgreSQL offsite backup 주기, retention, RPO와 RTO를 확정해야 한다.
- 무료 observability·object storage quota와 초과 시 동작을 결정해야 한다.

## ADR-0021 - Hetzner CX23에서 Production을 직접 운영한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

월 1만 원 상한에서 항상 실행되는 Spring Boot, PostgreSQL과 운영 증거를 제공하려면 유료 managed database·container·load balancer 조합을 사용하기 어렵다. OCI Always Free는 비용은 낮지만 리전별 ARM capacity 확보와 무료 정책에 의존한다. 사용자는 Hetzner 같은 VPS에서 Linux, container, database와 배포를 직접 구성하는 방식을 선호한다. Hetzner의 2026년 6월 이후 EU CX23은 shared 2 vCPU, 4GB RAM, 40GB SSD를 월 €5.49에 제공하고 IPv4는 월 €0.50이 추가된다.

### Decision

Hetzner EU 리전 CX23을 production 기본 compute로 사용하고 PostgreSQL을 포함한 runtime을 직접 운영한다.

- CX23 한 대에서 Caddy, Spring Boot, PostgreSQL, backup·maintenance와 경량 observability agent를 container별 resource limit으로 실행한다.
- React 정적 asset은 Spring release에 포함하고 public static·media delivery 앞에는 Cloudflare DNS/CDN을 둔다.
- Media binary와 암호화 PostgreSQL offsite backup은 Cloudflare R2에 저장한다.
- Terraform으로 Hetzner server, firewall, SSH key와 network resource를 선언하고 cloud-init 또는 Ansible로 OS·Docker·계정·directory·hardening을 재현한다.
- GitHub Actions는 test와 image build를 수행하고 GHCR에 immutable digest로 push한다. VPS에서는 image를 build하지 않는다.
- 배포 순간에만 현재·신규 Spring container를 함께 실행하고 신규 health·migration compatibility를 확인한 뒤 Caddy upstream을 전환한다.
- 실패 시 이전 immutable image digest와 호환 schema로 rollback한다.
- PostgreSQL port, Actuator privileged endpoint와 admin surface를 public network에 노출하지 않는다.
- SSH password와 root 직접 로그인을 금지하고 key·최소 권한 deploy account·host firewall·자동 security update를 적용한다.
- Disk, memory, CPU steal, container restart, certificate, health, backup age와 monthly cost를 monitor한다.
- IPv4 비용과 환율·card fee를 포함한 예상 원화 비용이 상한을 넘으면 IPv6-only origin과 Cloudflare proxy·관리 tunnel 구성을 사용한다.
- Singapore region은 현재 최저 plan이 예산을 넘으므로 사용하지 않고, EU network latency를 server time과 분리해 측정·문서화한다.
- OCI Always Free는 기본 topology가 아니라 Hetzner 장기 장애·계정 문제 시 평가할 수 있는 별도 복구 후보로만 남긴다.

### Alternatives

- OCI Always Free ARM: 더 많은 무료 자원을 제공하지만 capacity와 무료 정책 의존성이 있다.
- AWS Lightsail 1GB: AWS 경험과 예측 가능한 bundle은 장점이지만 Spring·PostgreSQL 동시 운영 메모리가 작고 환율상 비용 여유가 적다.
- Hetzner Singapore: 한국 latency는 낮아지지만 현재 월 €15.49 이상으로 비용 상한을 넘는다.
- 무료 PaaS + 무료 managed PostgreSQL: 관리 부담은 낮지만 sleep, cold start, quota와 provider별 제한이 운영·성능 증거를 흔든다.

### Consequences

- 운영 Linux, Docker, PostgreSQL, 배포와 장애 복구를 직접 다룬 검증 가능한 경험을 만든다.
- 단일 VPS와 local database가 단일 장애 지점이며 자동 failover 대신 offsite backup·restore가 필수다.
- Shared CPU와 EU network로 production latency가 변동하므로 controlled load test와 server timing을 별도로 유지해야 한다.
- 4GB RAM과 40GB disk 안에서 JVM, PostgreSQL, temporary dual container, log·image retention을 budget으로 관리해야 한다.
- OS·database patch, secret rotation, backup, disk cleanup과 security incident 대응이 애플리케이션 운영 범위에 포함된다.
- Euro 환율과 가격 변경으로 상한을 넘을 수 있어 월별 cost evidence와 downgrade·IPv6 전환 기준이 필요하다.

### Evidence

- Hetzner 공식 2026-06 가격표: EU CX23 월 €5.49, IPv4 별도 €0.50와 Singapore CPX12 월 €15.49를 안내한다.
- Hetzner Cloud 공식 문서: IPv6는 무료, IPv4는 월 €0.50이며 firewall·network·snapshot 기능을 제공한다.
- Cloudflare R2 공식 가격: standard storage 10GB-month와 일정 operation 무료 구간을 제공한다.
- 사용자 승인(2026-08-10): Hetzner CX23, 직접 운영 PostgreSQL과 Cloudflare R2 구성을 production 기본안으로 채택했다.

### Open Questions

- Hetzner EU 세부 location을 한국에서의 latency와 plan availability로 선택해야 한다.
- PostgreSQL physical·logical backup, WAL archive, RPO·RTO와 retention을 확정해야 한다.
- IPv4 포함 여부와 deploy management tunnel을 실제 원화 비용 계산 후 결정해야 한다.

## ADR-0022 - RPO 5분·RTO 60분과 검증된 PostgreSQL 복구 체계를 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Hetzner CX23 한 대에서 application과 PostgreSQL을 직접 운영하므로 VPS·disk·container·operator 실수가 핵심 단일 장애 지점이다. 단순 일일 `pg_dump`만 사용하면 최대 하루의 데이터가 손실되고 backup file이 실제로 복원 가능한지 알 수 없다. 반면 작은 포트폴리오 서비스에서 multi-region synchronous replica를 운영하면 비용 상한을 넘는다. 원장 데이터와 재구축 가능한 projection을 구분한 복구 목표가 필요하다.

### Decision

PostgreSQL production의 목표를 RPO 5분, 장애 인지 후 RTO 60분으로 정하고 WAL archive, logical·physical backup과 정기 restore 검증을 함께 운영한다.

- PostgreSQL WAL을 최대 5분 안에 암호화된 Cloudflare R2 offsite 경로로 archive해 point-in-time recovery를 지원한다.
- 매일 portable logical backup을 생성하고 주기적 physical base backup과 WAL chain으로 빠른 전체 복구를 준비한다.
- Backup은 VPS 밖에서 client-side encryption하고 encryption key를 backup storage·image·Git에 함께 보관하지 않는다.
- 각 backup에 source cluster, PostgreSQL·schema version, 시작·완료 시각, checksum, size와 WAL range manifest를 남긴다.
- WAL 7일, 일일 logical backup 14일, 주간 physical backup 8주, 검증된 월간 backup 6개월을 초기 retention으로 사용하고 실제 DB·R2 용량과 법적 보존 요구로 조정한다.
- Backup age, WAL archive failure·lag, checksum failure, R2 quota와 restore drill 결과를 alert한다.
- 매주 disposable PostgreSQL에 자동 restore·Flyway validation·핵심 query를 실행하고 매월 application smoke와 원장·projection 대사를 수행한다.
- 분기마다 새 VPS·빈 disk를 가정해 IaC bootstrap, secret 주입, base restore, WAL replay, application deploy와 DNS/proxy 전환까지 full recovery drill을 수행한다.
- 복구 후 Event Publication Registry backlog를 재개하고 SearchDocument·FeedDocument·EngagementSummary를 대사하거나 원장에서 rebuild한다.
- RTO는 감으로 보고하지 않고 탐지, provisioning, restore, migration, smoke와 traffic 전환 phase별 elapsed time을 evidence로 남긴다.
- RPO·RTO 위반 또는 backup chain 손상은 운영 incident로 기록하고 재발 방지 작업을 생성한다.

### Alternatives

- 일일 logical backup만 사용: 구현은 단순하지만 RPO가 최대 24시간이고 큰 DB 복구 시간이 길다.
- 같은 VPS의 snapshot만 사용: 복구는 빠를 수 있지만 account·region·operator·provider 장애와 함께 손실될 수 있다.
- 상시 PostgreSQL replica: RPO·RTO는 줄지만 두 번째 compute와 운영 비용이 상한을 넘는다.
- Backup 생성 성공만 점검: 비용은 낮지만 schema drift, key·checksum·WAL chain 오류로 실제 restore가 실패할 수 있다.

### Consequences

- 월 1만 원 안에서 자동 failover 대신 데이터 손실·복구 시간을 통제하고 증명한다.
- WAL archive, base backup, retention과 encryption key rotation 운영이 추가된다.
- R2 무료 용량을 초과하지 않도록 backup size·compression·retention을 지속 측정해야 한다.
- Full recovery drill이 일시적 Hetzner 자원·시간을 사용할 수 있으므로 비용 evidence와 정리 guard가 필요하다.
- Projection 재구축과 event 재처리가 중복 side effect를 만들지 않도록 멱등성이 필수다.

### Evidence

- `/Users/alex/project/townpet/business/operations/장애 대응 런북.md`: 기존 장애 탐지·복구와 기록 기준의 근거다.
- `/Users/alex/project/townpet/business/operations/운영_DB_demo_E2E_데이터_정리_절차.md`: 운영 DB 변경·검증 절차의 현재 근거다.
- ADR-0021: 단일 Hetzner VPS와 offsite R2가 필요한 production topology를 결정했다.
- 사용자 승인(2026-08-10): RPO 5분, RTO 60분과 WAL·logical·physical backup 및 restore 훈련을 채택했다.

### Open Questions

- WAL-G, pgBackRest 등 PostgreSQL·R2 호환 backup 도구를 restore benchmark 후 선택해야 한다.
- Encryption key 보관·rotation과 비상 접근 절차를 정해야 한다.
- Full drill용 임시 자원 비용을 정상 월 비용과 별도 실험 비용 중 어디에 산정할지 정해야 한다.

## ADR-0023 - 벤더 중립 계측과 외부 관측 저장소로 4GB VPS를 보호한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet production은 Spring Boot, PostgreSQL, Caddy와 배포 시점의 두 application container를 4GB Hetzner CX23 한 대에서 운영한다. 같은 서버에 Prometheus, Grafana, Loki와 trace backend까지 상시 운영하면 JVM·database가 사용할 memory와 disk I/O를 침해하고, 관측 시스템 장애가 본 서비스 장애를 악화시킬 수 있다. 반대로 외부 SaaS 전용 SDK에 애플리케이션을 결합하면 무료 구간·provider 변경 시 계측을 다시 작성해야 한다. 비용 상한을 지키면서도 application, domain event, database, host와 backup 상태를 함께 설명할 수 있는 관측 체계가 필요하다.

### Decision

애플리케이션은 Micrometer와 OpenTelemetry 표준으로 계측하고, VPS에는 Grafana Alloy 계열의 단일 경량 collector만 두며, 장기 조회·dashboard·alert 저장소는 무료 구간의 외부 관측 서비스를 사용한다.

- Spring Boot Actuator와 Micrometer로 HTTP, JVM, connection pool, cache와 application metric을 노출하고 metric 이름·label은 낮은 cardinality를 유지한다.
- OpenTelemetry context를 사용해 request, 비동기 event listener와 외부 호출을 연결하고 trace·span ID를 구조화 log에 포함한다.
- 정상 요청 trace는 sampling하고 오류·지연 요청은 우선 보존한다. sampling 비율과 tail 정책은 실제 quota와 진단 효용을 측정해 조정한다.
- Application은 JSON structured log를 stdout으로 출력하고 cookie, authorization, guest 관리 credential, OAuth token, password, 원문 IP·fingerprint와 민감한 사용자 내용을 수집 전에 redact한다.
- Grafana Alloy collector가 application metric·log·trace, PostgreSQL exporter, container와 node metric을 수집·batch·filter하여 외부 backend로 전송한다.
- 외부 기본 backend는 Grafana Cloud 무료 구간으로 시작하되, application code는 OTLP·Prometheus 호환 경계만 알아야 하며 provider 전용 API를 domain code에서 사용하지 않는다.
- VPS에는 collector outage를 견딜 수 있는 짧고 크기 제한된 local container log만 남기고 관측 backend 전체를 자체 호스팅하지 않는다.
- 최소 dashboard는 HTTP 성공률·latency, JVM heap·GC·thread, PostgreSQL pool·lock·slow query, host CPU·memory·disk·restart, event publication backlog·oldest age·failure, search·feed projection freshness, backup age·WAL lag, 배포 version과 authentication·rate-limit 이상을 포함한다.
- Alert는 사용자 영향과 복구 행동이 연결되는 항목만 page하고, 단순 metric 임계치 경고는 ticket 또는 dashboard 수준으로 분리한다.
- Alert와 dashboard 구성은 version control하고 release·migration·incident ID를 telemetry annotation으로 연결한다.
- 무료 quota의 metric series, log·trace ingest와 retention을 monitor한다. 상한 접근 시 debug log와 정상 trace sampling을 먼저 줄이며 오류, 보안, backup과 복구 신호는 보존한다.
- 외부 backend 장애가 application 요청을 실패시키지 않도록 모든 telemetry export는 bounded queue, timeout과 drop policy를 사용한다.

### Alternatives

- Prometheus·Grafana·Loki·Tempo를 CX23에 모두 자체 호스팅: 완전한 통제는 가능하지만 4GB memory와 40GB disk에서 본 서비스와 자원 경합이 크다.
- 외부 provider 전용 agent·SDK만 사용: 초기 설정은 쉽지만 계측과 query가 provider에 결합되고 비용·quota 변경에 취약하다.
- Application log와 Actuator health만 사용: 비용은 낮지만 module event 지연, database 병목, 배포 회귀와 backup 실패를 상관 분석하기 어렵다.
- 별도 관측 VPS 운영: 격리는 좋아지지만 월 1만 원 비용 상한을 넘긴다.

### Consequences

- 4GB production 서버의 자원을 본 서비스와 PostgreSQL에 우선 배분하면서 metric, log와 trace의 상관 분석이 가능해진다.
- 외부 무료 tier의 quota·retention·서비스 정책에 의존하므로 telemetry budget과 degradation 순서를 지속 관리해야 한다.
- Sampling 때문에 모든 정상 trace가 남지는 않지만 오류·지연·핵심 domain event를 우선 보존한다.
- Structured logging, context propagation, PII redaction과 low-cardinality review가 definition of done에 포함된다.
- Backend 교체 시 collector와 dashboard query 변환은 필요하지만 domain·application 계측 코드는 유지할 수 있다.

### Evidence

- Grafana Alloy 공식 문서: Prometheus와 OpenTelemetry 신호, application·infrastructure log를 수집하고 Grafana Cloud 또는 자체 backend로 전달하는 통합 collector임을 설명한다.
- ADR-0021: Spring Boot와 PostgreSQL이 4GB CX23의 제한된 자원을 공유하는 production topology를 결정했다.
- ADR-0022: backup age, WAL archive lag와 restore drill 결과를 운영 신호로 alert해야 한다.
- 사용자 승인(2026-08-10): Micrometer·OpenTelemetry, 경량 collector와 외부 무료 관측 backend 구성을 채택했다.

### Open Questions

- 사용자 경험과 운영 위험을 반영한 SLI, SLO와 error budget을 확정해야 한다.
- Grafana Cloud 무료 quota의 최신 수치와 계정별 제한을 production 연결 전에 공식 가격표에서 다시 검증해야 한다.
- 한국 사용자 관점의 외부 synthetic probe 위치와 무료 uptime service를 선택해야 한다.

## ADR-0024 - 측정 가능한 초기 SLO와 Error Budget으로 운영 품질을 관리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Metric, log와 trace를 수집하더라도 정상과 장애의 경계가 없으면 dashboard는 운영 결정을 만들지 못한다. TownPet은 EU의 단일 VPS에서 실행되므로 multi-AZ managed platform과 같은 99.9% 이상의 가용성을 선언하면 월 허용 장애 시간 약 44분이 RTO 60분과 모순된다. 또한 한국 사용자에게 보이는 network·browser latency와 application server 자체 처리시간을 분리하지 않으면 장거리 network를 backend 성능으로 오인하거나 실제 사용자 경험을 숨길 수 있다.

### Decision

TownPet production의 초기 30일 rolling SLO와 error budget 정책을 다음과 같이 사용한다.

- 공개 핵심 사용자 여정의 가용성 SLO는 99.5%로 한다. 외부 한국 synthetic probe가 로그인 없이 수행하는 health가 아니라 실제 read 여정과 별도의 authenticated canary 여정을 측정한다.
- 핵심 API의 server-side 성공률은 유효 요청 중 5xx가 아닌 응답 99.5% 이상으로 한다. 예상된 validation·authorization 4xx와 dependency failure는 별도 분류하고 임의로 성공률을 높이기 위해 제외하지 않는다.
- Controlled load 조건에서 application server 처리시간은 read API p95 300ms 이하, write API p95 500ms 이하로 한다. Query 종류별 별도 budget이 필요한 endpoint는 contract와 performance test에 명시한다.
- 한국 사용자 실제 경험은 mobile p75 기준 LCP 2.5초 이하, INP 200ms 이하, CLS 0.1 이하를 목표로 하고 server timing과 분리해 수집한다.
- Event Publication Registry에서 시작하는 비동기 projection 반영은 p95 30초 이하, 처리 가능한 정상 상태에서 oldest backlog age 60초 이하를 목표로 한다.
- WAL archive lag는 5분 이하, 마지막으로 검증된 일일 backup age는 24시간 이하를 유지한다.
- 사용자 영향 장애는 최초 실패부터 5분 안에 탐지하는 것을 목표로 한다.
- 계획된 점검도 사용자가 핵심 여정을 수행하지 못하면 가용성 실패로 계산한다. 단, 사전에 정의한 내부 admin·실험 환경은 production SLI 대상이 아니다.
- Client 4xx, OAuth·object storage 같은 외부 dependency 장애, rate limit, bot·공격 traffic과 telemetry backend 장애는 각각 별도 SLI로 유지한다. 제외 기준은 version control하고 incident마다 근거를 남긴다.
- 99.5% 가용성은 30일 동안 약 216분의 error budget을 제공한다. 30일 budget의 50%가 소진되거나 빠른 burn-rate alert가 발생하면 고위험 release를 제한하고, 100%가 소진되면 긴급 보안·복구 작업 외 기능 release를 중단해 안정화와 재발 방지를 우선한다.
- SLO 분모, query, probe, 제외 규칙과 dashboard는 code review 대상인 설정으로 관리하며 실제 traffic이 충분하지 않은 초기에는 synthetic와 controlled test 결과를 함께 표시한다.
- SLO는 초기 운영 자료가 쌓인 뒤 완화가 아니라 사용자 가치·비용·실측 분포를 근거로 분기별 review한다.

### Alternatives

- 99.9% 이상 가용성 선언: 보기에는 좋지만 단일 VPS, RTO 60분과 비용 상한에서 신뢰할 수 없는 목표다.
- Uptime health endpoint만 SLI로 사용: process가 응답해도 database·auth·핵심 read 여정이 실패하는 장애를 놓친다.
- 평균 latency 사용: tail latency와 일부 사용자의 심각한 지연을 숨긴다.
- 관측만 하고 error budget을 사용하지 않음: 수치는 남지만 배포·안정화 의사결정에 영향을 주지 않는다.

### Consequences

- 제한된 topology의 위험을 숨기지 않으면서 사용자 관점 품질을 정량적으로 설명할 수 있다.
- Synthetic probe, authenticated canary, real-user Web Vitals와 server timing 수집이 필요하다.
- 외부 dependency와 악성 traffic 분류 규칙을 엄격히 관리하지 않으면 SLI를 유리하게 왜곡할 수 있다.
- Error budget 소진 시 기능 일정이 늦어질 수 있지만 안정성 부채를 지속적으로 방치하지 않는다.
- Traffic이 적은 초기에는 통계적 표본이 작으므로 synthetic·controlled test와 실제 사용자 지표를 구분해 제시해야 한다.

### Evidence

- ADR-0021: EU 단일 CX23이 production의 비용·가용성 제약임을 결정했다.
- ADR-0022: RPO 5분, RTO 60분과 backup 검증 목표를 결정했다.
- ADR-0023: SLI를 계산할 metric, log, trace와 운영 dashboard 수집 체계를 결정했다.
- 사용자 승인(2026-08-10): 초기 가용성·성공률·latency·Web Vitals·projection·backup SLO와 error budget 정책을 채택했다.

### Open Questions

- 핵심 사용자 여정과 핵심 API 목록을 parity matrix에서 표시해야 한다.
- 한국 synthetic probe와 authenticated canary의 provider·credential 관리 방식을 선택해야 한다.
- Controlled load의 dataset, concurrency와 warm-up 조건을 endpoint별 performance specification으로 확정해야 한다.

## ADR-0025 - Java 25 LTS와 Spring Boot 4.1을 기술 기준선으로 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet은 기존 application의 단기 수정이 아니라 2026년에 새로 시작하는 장기 유지 프로젝트다. Java 21과 Spring Boot 3.5는 안정적인 이전 세대지만 Java 25가 최신 LTS로 출시됐고 Spring Boot 4.1이 현재 안정판이다. Spring Modulith 2.1도 Boot 4.1 기준선을 지원한다. 최신 non-LTS·preview 기능을 좇으면 재현성과 라이브러리 호환성이 약해지고, 반대로 이미 이전 세대가 된 기준선을 택하면 신규 프로젝트의 유지 기간과 최신 Spring 역량을 충분히 보여주지 못한다.

### Decision

Backend 기술 기준선을 Eclipse Temurin Java 25 LTS, Spring Boot 4.1.x, Spring Framework 7.0.x와 Spring Modulith 2.1.x로 정한다.

- Production과 CI의 JDK distribution은 Eclipse Temurin 25로 통일하고 Gradle Java toolchain과 container base image로 강제한다.
- Application source는 Java로 작성한다. Build script의 Kotlin DSL은 application 언어 선택으로 간주하지 않는다.
- Gradle 9 Wrapper와 Kotlin DSL을 사용하고 개발자가 설치한 system Gradle에 의존하지 않는다.
- Preview·incubator API와 `--enable-preview`를 production 기준선에서 사용하지 않는다.
- Spring Boot dependency management가 관리하는 dependency는 개별 version override를 최소화하고 호환 BOM을 우선한다.
- Spring Boot, Modulith와 보안 patch는 고정된 version으로 재현하되 자동 update PR을 생성하고 전체 quality gate 통과 후 병합한다.
- Virtual thread, GraalVM native image와 AOT는 기본 architecture가 아니다. 기존 thread model 대비 throughput, tail latency, memory, startup과 운영 복잡성을 benchmark한 뒤 별도 ADR로만 도입한다.
- Java runtime, Gradle wrapper, Node build tool과 container image의 정확한 version·digest를 release provenance에 기록한다.
- Major·minor framework upgrade는 자동 병합하지 않고 migration note, dependency compatibility, schema·contract·performance regression 결과를 review한다.

### Alternatives

- Java 21 + Spring Boot 3.5: 자료와 현업 도입 사례가 많지만 신규 장기 프로젝트에서 이전 Spring 세대를 선택하는 비용이 생긴다.
- Java 25 + Spring Boot 3.5: Java LTS는 최신이지만 Spring Framework 7·Boot 4의 현재 API와 운영 모델을 사용하지 못한다.
- Java 26: 현재 기능 release로 지원 기간이 짧아 장기 기준선에 부적합하다.
- Preview 기능 적극 사용: 언어 실험을 보여줄 수 있지만 build·runtime flag와 차기 release migration 부담이 production 신뢰성을 해친다.
- Maven: 단순하고 보편적이지만 이 프로젝트는 Gradle toolchain, task 구성과 frontend·contract generation pipeline 통합을 우선한다.

### Consequences

- 최신 LTS Java와 안정 Spring generation을 사용해 장기 유지성과 현대적 backend 역량을 함께 보여준다.
- Spring Boot 3 기반 예제와 library를 그대로 적용할 수 없어 공식 Boot 4·Framework 7 migration 차이를 확인해야 한다.
- 4GB VPS에서 Java 25 JVM의 heap, native memory와 container awareness를 실제 측정해 제한해야 한다.
- Gradle Kotlin DSL을 유지해야 하지만 production application code와 domain model은 Java로 일관된다.
- Dependency 자동 갱신이 merge되기 전에 architecture, contract, integration, performance와 browser parity test 비용이 발생한다.

### Evidence

- Spring Boot 4.1 공식 system requirements: Java 17 이상과 Java 26까지의 호환성, Gradle 8.14 이상 또는 9.x 지원을 명시한다.
- Oracle Java SE support roadmap: Java 25가 2025년 9월 출시된 최신 LTS이며 Java 21은 이전 LTS임을 명시한다.
- Spring Modulith 2.1 공식 문서: Spring Boot 4.1 세대와 module verification·testing·runtime 기능을 제공한다.
- 사용자 승인(2026-08-10): Java 25, Spring Boot 4.1, Spring Modulith 2.1과 Gradle 9 기준선을 채택했다.

### Open Questions

- JPA, jOOQ 또는 Spring Data JDBC를 역할별로 선택하는 영속성 전략을 확정해야 한다.
- JVM heap·native memory와 container별 resource limit을 load test로 결정해야 한다.
- Dependency update PR의 merge cadence와 긴급 보안 patch SLA를 정해야 한다.

## ADR-0026 - JPA Write Model과 jOOQ Read Model을 Flyway Schema 위에서 함께 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet에는 aggregate invariant를 지키는 회원·게시물·거래·모임·신고 등의 쓰기 흐름과 feed, 검색, 목록, moderation queue, 운영 통계처럼 PostgreSQL 기능과 실행 계획 제어가 중요한 조회 흐름이 함께 존재한다. 모든 작업을 JPA로 처리하면 복잡한 projection과 PostgreSQL 전용 검색·집계 SQL이 우회적으로 변하고 N+1·과도한 entity graph 위험이 커진다. 반대로 모든 작업을 SQL mapper로 구현하면 aggregate lifecycle, optimistic locking과 Spring Data JPA 역량을 충분히 보여주기 어렵다. ORM schema 자동 생성과 migration을 동시에 사용하면 production schema의 주인이 둘이 되는 문제도 있다.

### Decision

명령 측의 aggregate 영속성은 Spring Data JPA와 Hibernate, 조회·projection은 jOOQ를 사용하고 Flyway migration을 database schema의 유일한 권위로 둔다.

- Aggregate 생성·상태 전이·invariant와 일반 write는 module 내부 JPA entity와 repository를 통해 수행한다.
- Feed, search, 목록, moderation·operations 조회, reporting과 PostgreSQL 특화 query는 jOOQ record를 application 전용 read model로 mapping한다.
- jOOQ read path는 기본적으로 source table을 변경하지 않는다. 대량 repair·backfill처럼 SQL write가 필요한 작업은 별도 operation use case, 권한, idempotency, audit와 ADR 또는 명시적 설계를 요구한다.
- Flyway versioned migration이 table, constraint, index, extension, trigger와 view를 관리한다. Hibernate DDL 생성·수정 기능은 사용하지 않고 모든 환경에서 `ddl-auto=validate`로 mapping drift를 조기에 실패시킨다.
- JPA entity, Spring Data repository와 jOOQ table 접근 구현은 해당 bounded context의 infrastructure 내부에 둔다. 다른 module은 공개 application API·event만 사용한다.
- Aggregate 사이와 module 사이에 JPA association을 만들지 않고 typed identifier 값으로 참조한다. Aggregate 내부에서도 cascade와 collection loading 범위를 작고 명시적으로 유지한다.
- Open Session in View를 비활성화하고 transaction은 application use case가 명시적으로 연다. Web serialization이 lazy loading을 유발하지 않게 transport DTO를 별도로 사용한다.
- Repository는 외부 API에서 JPA entity를 반환하지 않고 domain object, application result 또는 전용 port type으로 변환한다.
- Fetch join, entity graph, batch size와 jOOQ query는 예상 row 수와 query count가 드러나는 integration test로 검증한다. N+1은 review 관례가 아니라 query-count failure로 탐지한다.
- jOOQ type-safe source는 Flyway migration이 적용된 disposable PostgreSQL schema에서 생성하고 schema drift가 있으면 CI가 실패한다. 생성물 보관 방식은 local build 재현성과 diff noise를 비교해 build bootstrap에서 확정한다.
- Repository, migration, PostgreSQL extension, lock와 query semantics test는 실제 PostgreSQL Testcontainers를 사용한다. H2 등 다른 database를 production 대체 test로 사용하지 않는다.
- 핵심·고비용 query에는 대표 dataset, `EXPLAIN (ANALYZE, BUFFERS)`, 예상 index와 latency budget을 evidence로 보관하고 schema·query 변경 시 회귀를 확인한다.

### Alternatives

- JPA 단독: aggregate에는 적합하지만 feed·검색·집계와 PostgreSQL 특화 SQL을 숨기거나 비효율적인 object graph로 만들기 쉽다.
- jOOQ 단독: SQL 통제는 뛰어나지만 ORM aggregate lifecycle과 Spring Data JPA 설계·운영 경험을 보여주지 못한다.
- Spring Data JDBC 단독: aggregate 경계가 명확하지만 기존 채용 생태계에서 요구되는 JPA 경험과 복잡 read query 지원을 별도로 보완해야 한다.
- Hibernate DDL auto + Flyway 병행: 개발 초기에는 편하지만 schema 변경의 주인이 둘이 되어 환경 drift와 비재현 migration을 만든다.
- H2 repository test: 빠르지만 PostgreSQL lock, extension, type, index와 SQL 동작을 충실히 검증하지 못한다.

### Consequences

- JPA의 aggregate·transaction 장점과 jOOQ의 type-safe SQL·실행 계획 통제를 함께 보여줄 수 있다.
- 같은 schema를 두 persistence 기술로 mapping하므로 model·mapping·code generation 관리 비용이 늘어난다.
- Write invariant를 우회하지 않도록 jOOQ write 사용을 architecture test와 review로 제한해야 한다.
- Local·CI build에서 PostgreSQL 기반 code generation과 Testcontainers를 실행할 Docker 환경이 필요하다.
- OSIV 비활성화로 web layer가 entity를 직접 직렬화하는 편의는 사라지지만 query boundary와 API contract가 명확해진다.

### Evidence

- ADR-0008: 공통 Publication과 domain aggregate를 ID로 연결하고 cross-module JPA association을 금지했다.
- ADR-0015와 ADR-0016: PostgreSQL SearchDocument·FeedDocument와 특화 query·index를 사용한다.
- ADR-0025: Java 25, Spring Boot 4.1, Gradle 9 기술 기준선을 사용한다.
- 사용자 승인(2026-08-10): JPA write model, jOOQ read model, Flyway schema authority와 PostgreSQL Testcontainers 전략을 채택했다.

### Open Questions

- PostgreSQL version, identifier, timestamp, money와 concurrency convention을 확정해야 한다.
- jOOQ generated source를 commit할지 build에서 항상 생성할지 build time·재현성 실험으로 선택해야 한다.
- Query-count assertion library 또는 DataSource instrumentation 방식을 선택해야 한다.

## ADR-0027 - PostgreSQL 18과 UUIDv7·UTC·원 단위·명시적 동시성 규칙을 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet 데이터는 여러 domain에서 식별자, timestamp, 금액, 상태와 JSON을 사용한다. 재작성 과정에서 각 module이 임의의 타입·시간대·locking 방식을 선택하면 API와 migration이 불일치하고, 경합 상황에서 중복 참가·초과 판매·lost update가 발생할 수 있다. PostgreSQL 18은 2025년 9월 공개된 현재 안정 major이며 2030년까지 지원되고 native `uuidv7()`을 제공한다. 최신 기능을 사용하더라도 데이터 타입과 concurrency 의미는 application·database·API 전반에 명시적으로 통일해야 한다.

### Decision

PostgreSQL 18의 최신 security·bug-fix minor를 production 기준으로 사용하고 다음 데이터·동시성 convention을 모든 module에 적용한다.

- Local Docker, CI Testcontainers, migration 검증과 production의 PostgreSQL major를 18로 통일한다. Production image는 검증한 minor와 digest로 고정하고 minor update를 자동 무검증 적용하지 않는다.
- Domain identifier는 PostgreSQL native `uuid`에 저장하는 UUIDv7을 기본으로 한다. Aggregate는 persist·event 발행 전에 application에서 UUIDv7을 생성하고, database가 직접 만드는 운영 record는 `DEFAULT uuidv7()`을 사용할 수 있다.
- API와 event에서 UUID는 lowercase canonical string으로 표현하며 sequence·row count를 외부 identifier로 노출하지 않는다.
- 절대 시점은 PostgreSQL `timestamptz`와 Java `Instant`로 저장하고 DB session, JVM과 container timezone은 UTC로 통일한다. 사용자 화면에서만 명시적 zone으로 변환한다.
- 생일, 휴무일, 행사 기준일처럼 시간대와 무관한 달력 값은 `date`와 `LocalDate`를 사용한다. 예약처럼 지역 timezone 규칙이 의미 있는 경우 `Instant`와 IANA zone ID를 함께 보존한다.
- KRW 금액은 원 단위 `bigint`와 음수·상한 check constraint로 저장하고 Java에서는 단위와 연산을 캡슐화한 value object를 사용한다. 금액에 binary floating point를 사용하지 않는다.
- 핵심 invariant는 application 검사만 믿지 않고 `NOT NULL`, `CHECK`, `UNIQUE`, `FOREIGN KEY`, partial unique index와 조건부 update로 database에서도 보호한다.
- 변경 가능한 aggregate root에 version column과 optimistic locking을 적용한다. 충돌은 무조건 덮어쓰지 않고 API conflict 응답과 사용자 재시도 의미를 정의한다.
- 기본 transaction isolation은 PostgreSQL `READ COMMITTED`로 유지한다. 참가 정원, 중복 reaction, 재고·예약처럼 경합하는 invariant는 unique constraint, compare-and-set 또는 조건부 atomic update를 우선한다.
- Pessimistic lock은 희소 자원이나 복수 row의 강한 순서 보장이 필요한 짧은 transaction에만 사용하고, 여러 자원은 안정된 identifier 순서로 lock한다.
- Serialization failure, deadlock과 transient connection failure는 idempotent operation에 한해 제한된 횟수와 jitter로 재시도하며 retry exhaustion을 metric·error로 노출한다.
- Domain 상태는 migration이 어려운 PostgreSQL enum보다 `varchar`와 check constraint 또는 owner table을 기본으로 한다. Java enum 이름을 무검토로 영구 wire·storage contract로 사용하지 않는다.
- `jsonb`는 audit snapshot, provider별 metadata와 실제 가변 payload에만 사용한다. 검색·join·constraint와 핵심 business rule에 필요한 field를 JSON에 숨기지 않는다.
- 모든 table에 획일적 soft-delete column을 추가하지 않고 aggregate별 lifecycle, retention, visibility와 legal hold를 모델링한다.
- Audit timestamp는 `created_at`, `updated_at` naming을 사용하되 database trigger가 business state를 암묵적으로 바꾸지 않는다. Trigger는 무결성·기술 metadata에 제한하고 문서·test한다.
- Schema 변경은 Flyway expand/contract, backward-compatible release와 forward-fix를 기본으로 하며 rollback SQL을 형식적으로 강요하지 않는다.
- PostgreSQL minor update 전 offsite backup 상태, disposable restore, Flyway validation, repository·query regression과 extension 호환성을 확인한다.

### Alternatives

- PostgreSQL 17: 더 오래 운영된 안정성은 있지만 신규 프로젝트에서 PostgreSQL 18의 UUIDv7·관측·성능 개선을 포기한다.
- PostgreSQL 19 beta: 최신 기능은 시험할 수 있지만 production 안정판이 아니고 migration 기준선으로 부적합하다.
- Sequence `bigint` ID: 작고 빠르지만 외부 노출 시 enumeration이 쉽고 application-side aggregate ID 생성·data merge가 불편하다.
- UUIDv4: 분산 생성은 쉽지만 무작위 B-tree insertion locality가 UUIDv7보다 불리하다.
- 모든 상태를 PostgreSQL enum·모든 가변 데이터를 JSONB로 저장: 초기 schema는 간단하지만 evolution, constraint와 query 명료성이 약해진다.
- 기본 SERIALIZABLE isolation: 일부 anomaly를 막지만 전체 transaction abort·retry 비용이 커지고 business invariant별 의도가 숨겨진다.

### Consequences

- Application, API, event와 database의 식별자·시간·금액 의미가 일관되고 migration mapping이 명확해진다.
- UUIDv7 generator library의 정확성, monotonicity와 clock rollback 동작을 test해야 한다.
- Optimistic conflict, constraint violation, deadlock과 retry exhaustion을 정상적인 API·운영 시나리오로 구현해야 한다.
- PostgreSQL 18 전용 `uuidv7()`과 일부 기능으로 이전 major 호환성은 의도적으로 포기한다.
- Domain별 lifecycle과 retention을 설계해야 하므로 획일적 soft delete보다 초기 modeling 비용이 크다.

### Evidence

- PostgreSQL 공식 versioning policy: PostgreSQL 18은 지원 중이며 2030년 11월까지 지원되고 현재 minor가 18.4임을 명시한다.
- PostgreSQL 18 공식 UUID 문서: native `uuidv7()`과 timestamp extraction을 지원한다.
- ADR-0008: Aggregate는 객체 연관 대신 identifier로 연결한다.
- ADR-0012: API에서 identifier는 string, timestamp는 UTC, money는 integer로 표현한다.
- 사용자 승인(2026-08-10): PostgreSQL 18, UUIDv7, UTC·KRW convention과 명시적 locking·constraint 전략을 채택했다.

### Open Questions

- UUIDv7 Java implementation과 clock rollback test vector를 선택해야 한다.
- Korean text collation·normalization과 `citext`, `pg_trgm`, full-text configuration을 migration rehearsal에서 확정해야 한다.
- Aggregate별 optimistic conflict UX와 scarce-resource lock 전략을 domain state machine에서 구체화해야 한다.

## ADR-0028 - RBAC와 Resource 속성 정책을 결합해 모든 Use Case를 기본 거부한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet의 권한은 로그인 여부나 전역 관리자 role만으로 결정되지 않는다. 작성자, guest 관리 credential, publication lifecycle, visibility restriction, 회원 제재, 상호 차단 관계, 모임·거래 상태와 moderator의 조치 범위가 함께 작동한다. Controller마다 `isAdmin`과 `authorId` 비교를 반복하면 동일한 정책이 endpoint별로 달라지고 resource ID만 바꿔 다른 사용자의 정보를 읽거나 변경하는 IDOR가 발생하기 쉽다. 반대로 모든 권한을 하나의 중앙 서비스에 모으면 각 bounded context의 business 상태를 침범하는 거대한 조건문이 된다.

### Decision

TownPet은 coarse-grained RBAC와 bounded-context별 resource attribute policy를 결합하고 모든 application use case를 deny by default로 처리한다.

- Security principal은 `ANONYMOUS`, `GUEST`, `MEMBER`, `MODERATOR`, `OPERATOR`, `ADMIN` actor category와 안정된 actor ID, session·assurance 정보를 제공한다.
- Role은 endpoint·use case 진입의 큰 범위만 결정하고 최종 허용은 ownership, aggregate state, publication effective visibility, active restriction, sanction, block relationship과 관리 scope를 함께 평가한다.
- Controller의 Spring Security annotation은 authentication과 coarse role gate까지만 담당한다. Resource별 판단은 해당 bounded context의 application authorization policy가 aggregate·read model을 사용해 수행한다.
- Controller, mapper와 repository에 `isAdmin`, author 비교와 block 우회 조건을 중복 구현하지 않는다. Repository에서 row를 찾았다는 사실도 권한 허용을 뜻하지 않는다.
- 사용자가 존재 자체를 알 권한이 없는 private resource는 외부에서 원칙적으로 `404`로 응답한다. 공개된 resource에 특정 action만 금지된 경우는 `403`으로 구분하고 내부 reason은 안전한 ProblemDetail code로 제한한다.
- Block relationship은 공개 정책에 따라 listing·상세·comment·reaction·bookmark·contact·gathering·marketplace 상호작용에 일관되게 적용하며 discovery projection도 같은 effective policy를 반영한다.
- 작성자는 자기 publication lifecycle을 변경할 수 있지만 moderator의 visibility restriction이나 audit record를 삭제·해제할 수 없다.
- `MODERATOR`는 report 판정과 콘텐츠·상호작용 제한만 수행하고 account privilege, deployment, secret, backup·event replay 권한을 갖지 않는다.
- `OPERATOR`는 deployment, restore, projection rebuild와 event retry 등 운영 기능만 수행하고 콘텐츠 판정·회원 역할 승격을 할 수 없다.
- `ADMIN`도 domain invariant, step-up, audit와 dual-control 대상 action을 우회하지 않는다. Superuser 논리를 일반 request path에 두지 않는다.
- 위험한 staff action은 강한 재인증, MFA assurance, 구조화된 사유, request·decision ID와 append-only audit를 요구한다.
- Staff identity와 credential은 일반 회원 OAuth account와 분리하고 최소 권한·만료·회수 가능한 방식으로 provisioning한다.
- User impersonation은 금지한다. 향후 지원 필요성이 입증되면 사용자 동의, 시간 제한, 화면 표시, 최소 scope와 완전한 audit를 갖춘 별도 ADR 없이는 도입하지 않는다.
- Application이 한 owner DB role로 동작하는 비 multi-tenant 구조이므로 PostgreSQL RLS를 기본 보안 경계로 사용하지 않는다. 필요하면 module schema role 분리는 defense-in-depth로 별도 검토한다.
- 역할·ownership·resource state·block·restriction 조합을 authorization matrix로 만들고 positive·negative test, 다른 사용자의 ID로 교체하는 IDOR contract test와 projection leakage test를 자동화한다.
- 거부 응답과 log는 block 여부, private state, moderation evidence와 내부 policy 식을 공격자에게 노출하지 않는다.
- Staff authorization decision은 actor, action, resource, policy version, reason, outcome, request·trace ID와 timestamp로 감사 가능하게 기록한다.

### Alternatives

- Controller별 role·owner 조건문: 구현은 빠르지만 endpoint와 module마다 정책 drift와 IDOR가 발생한다.
- 전역 `AuthorizationService` 하나에 모든 규칙 집중: 호출은 단순하지만 bounded context의 상태와 용어를 한 거대한 정책 계층이 소유하게 된다.
- RBAC만 사용: 역할 수가 폭증하고 자원 소유권·상태·관계별 결정을 표현하지 못한다.
- PostgreSQL RLS를 주 경계로 사용: multi-tenant가 아닌 single application role에서 request actor 전달·pool context 관리가 복잡하고 application 정책 누락을 완전히 해결하지 못한다.
- Admin 전면 bypass: 운영은 편하지만 실수·계정 탈취의 blast radius와 감사 공백이 커진다.

### Consequences

- 인증 성공과 business authorization을 분리해 IDOR와 상태 우회 규칙을 일관되게 test할 수 있다.
- 각 module이 authorization policy와 matrix를 유지해야 하며 상태가 추가될 때 negative test도 갱신해야 한다.
- Projection과 cache가 policy 변경을 늦게 반영하면 정보가 노출될 수 있어 restriction·block event의 freshness와 fail-closed 처리가 중요하다.
- Staff identity, MFA, privilege provisioning과 audit 조회 기능을 별도 운영 범위로 구현해야 한다.
- `404`와 `403` 선택이 endpoint마다 임의적이지 않도록 resource exposure policy를 contract에 문서화해야 한다.

### Evidence

- ADR-0009와 ADR-0010: Member session과 GuestPrincipal·관리 credential을 분리했다.
- ADR-0018: Publication lifecycle과 multiple VisibilityRestriction을 분리하고 moderator API·audit를 요구한다.
- ADR-0011: Identity, Relationship, TrustSafety와 Operations가 별도 bounded context로 존재한다.
- 사용자 승인(2026-08-10): deny-by-default RBAC+resource attribute authorization, staff 역할 분리와 IDOR matrix test를 채택했다.

### Open Questions

- Staff MFA mechanism, emergency access와 privilege provisioning workflow를 확정해야 한다.
- Block 관계가 기존 화면별로 어떤 노출·상호작용을 막는지 parity matrix에서 정밀 inventory해야 한다.
- 위험 작업 중 dual-control을 요구할 정확한 action 목록을 threat model에서 결정해야 한다.

## ADR-0029 - 공개 배포는 실제 Community가 아닌 기능 완전형 Portfolio Sandbox로 운영한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

TownPet Spring의 목적은 실제 사용자를 빠르게 모집하는 서비스 출시가 아니라 Spring Boot architecture, migration, 운영과 품질 역량을 증명하는 완성도 높은 포트폴리오다. 공개 URL에서 자유 회원가입·social OAuth와 영구 콘텐츠를 받으면 규모가 작아도 실제 개인정보 처리, 국외 이전, 민원·삭제 요청과 콘텐츠 운영 책임이 생긴다. 반대로 모든 기능을 read-only mock으로 만들면 인증, 권한, transaction, concurrency, event, media와 운영 능력을 검증할 수 없다. 제품 코드는 완전하게 유지하면서 공개 환경의 데이터와 사용자 범위를 제한할 필요가 있다.

### Decision

Hetzner에 공개하는 production topology의 운영 성격을 `showcase` portfolio sandbox로 정의하고, 실제 개인정보 community launch와 분리한다.

- Application의 회원가입, OAuth, guest write, publication, engagement, marketplace, gathering, moderation, media와 운영 use case는 제품 수준으로 구현하고 automated test에서 완전하게 검증한다.
- 공개 showcase에서는 일반 회원가입과 Kakao·Naver OAuth 연결을 비활성화하고 문서화된 역할별 demo account만 제공한다.
- Showcase database의 기준 데이터는 deterministic synthetic fixture 또는 검증된 anonymized fixture만 사용하고 원본 production credential·개인정보를 반입하지 않는다.
- Demo account의 password·권한·상태는 seed manifest로 재현하고 운영자·관리자 위험 기능은 public credential로 제공하지 않는다.
- 방문자는 허용된 demo account로 글, 댓글, reaction, 거래, 모임과 제한된 media upload를 실제 transaction으로 체험할 수 있다.
- 방문자가 생성한 콘텐츠·업로드·session과 파생 projection은 생성 후 최대 24시간 안에 만료·삭제하고 object storage orphan까지 reconciliation한다.
- Showcase 화면의 로그인·작성·upload 지점에 포트폴리오 데모임과 실제 개인정보·민감정보를 입력하지 말아야 함, 데이터가 자동 삭제됨을 명확히 표시한다.
- Upload는 image MIME·magic byte, pixel·byte limit, malware 방어와 rate limit을 거친 허용 형식만 받고 공개 showcase에서는 위치 metadata 등 불필요한 EXIF를 제거한다.
- 정기 reset은 임의 SQL overwrite가 아니라 versioned seed, cleanup use case와 idempotent job으로 실행하며 reset 중 사용자에게 상태를 표시한다.
- Showcase read fixture에는 핵심 상태와 edge case가 보이도록 충분한 scenario를 유지하되, 동시 방문자의 체험을 깨지 않도록 demo actor별 또는 scenario namespace별 write 격리를 우선 검토한다.
- Local·CI·review environment에서는 public showcase에서 꺼진 signup·OAuth·탈퇴·export를 provider stub과 sandbox credential로 검증한다.
- Public showcase에도 TLS, patch, backup·restore, monitoring, audit, rate limit, abuse 방어, SLO와 incident runbook을 그대로 적용한다. 포트폴리오라는 이유로 보안 기준을 낮추지 않는다.
- 문서에서 `production`이라는 용어가 공개 배포 topology를 가리킬 때는 현재 `showcase production`을 의미한다. 실제 community launch는 개인정보·약관·국외 이전·moderation staffing과 retention readiness review를 통과하는 별도 decision 없이는 허용하지 않는다.

### Alternatives

- 자유 가입 가능한 실제 community 운영: 제품 현실성은 높지만 포트폴리오 범위를 넘어 개인정보·국외 이전·상시 운영 책임이 생긴다.
- 완전 read-only demo: 안전하고 단순하지만 write transaction, authorization, event와 concurrency 동작을 사용자가 확인할 수 없다.
- Frontend mock data만 사용: UI는 보일 수 있지만 Spring Boot backend와 PostgreSQL architecture를 공개 환경에서 증명하지 못한다.
- 매 요청마다 database 전체 reset: 상태는 일정하지만 동시 사용자 경험과 transaction 검증을 깨고 운영 위험이 크다.

### Consequences

- 실제 개인정보 수집을 최소화하면서도 동작하는 backend, database와 운영 자동화를 공개적으로 증명할 수 있다.
- Showcase flag가 product behavior와 갈라져 숨은 미구현을 만들지 않도록 같은 binary·use case를 사용하고 entry policy만 달리해야 한다.
- Public demo write의 abuse, storage quota와 사용자 간 간섭을 방지하는 namespace·expiration 설계가 필요하다.
- OAuth provider의 실제 redirect flow는 공개 환경에서 보이지 않으므로 E2E evidence와 영상·문서가 필요하다.
- 향후 실제 출시 시 법률·개인정보·운영 readiness를 별도 project로 수행해야 하며 showcase 설정만 끄는 것으로 출시할 수 없다.

### Evidence

- 사용자 설명(2026-08-10): TownPet Spring은 실제 community 운영보다 취업 포트폴리오로 사용할 프로젝트다.
- ADR-0019: 기능 완성도는 기존 TownPet과의 parity matrix와 differential·visual test로 판단한다.
- ADR-0021부터 ADR-0024: 공개 배포에도 VPS 운영, backup, observability와 SLO 증거를 유지한다.
- 사용자 승인(2026-08-10): 공개 환경을 기능 완전형 portfolio sandbox로 운영하고 실제 개인정보 가입은 차단하는 방식을 채택했다.

### Open Questions

- Demo account·scenario namespace와 reset 단위를 확정해야 한다.
- OAuth flow를 채용 담당자가 확인할 수 있는 안전한 evidence 형식을 선택해야 한다.
- Showcase configuration이 실제 launch configuration으로 실수 전환되지 않도록 release guard를 설계해야 한다.

## ADR-0030 - 공개 Showcase는 고정 Demo 계정과 주기적 Seed 복구를 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

방문자별 lease·임시 actor·scenario namespace는 동시 체험 격리를 제공하지만 domain table과 운영 흐름에 portfolio 전용 multi-tenancy 성격을 추가한다. 사용자는 공개 showcase에서도 별도 체험 abstraction보다 일반 계정 로그인 방식을 원한다. 공용 계정은 동시 접속과 상태 변경 충돌을 허용하지만 예상 traffic이 낮고 주기적 복구가 가능한 portfolio 환경에서는 단순성과 실제 authentication flow 재사용의 이점이 더 크다.

### Decision

Showcase는 일반 Credentials authentication을 그대로 사용하는 소수의 고정 demo 계정을 제공하고 별도 DemoLease·visitor namespace를 구현하지 않는다.

- `demo-member1`, `demo-member2`, `demo-member3`처럼 여러 고정 MEMBER 계정을 제공하고 credential은 showcase 로그인 화면과 README에 공개한다.
- Demo 계정도 일반 Spring Security credentials authentication, server session, CSRF와 authorization policy를 그대로 통과한다. Demo 전용 우회 로그인 API를 만들지 않는다.
- 각 MEMBER 계정에 소유 publication, marketplace item, gathering과 edge state fixture를 배정해 수정·삭제·상태 전이를 직접 체험하게 한다.
- 제한된 `demo-moderator` 계정은 showcase demo resource에만 moderation action을 수행할 수 있게 하되 privilege·member role·operations·infrastructure 기능은 갖지 않는다.
- `ADMIN`, `OPERATOR`, emergency account와 실제 staff credential은 공개하지 않는다. 해당 기능은 automated evidence와 민감정보를 제거한 read-only artifact로 제시한다.
- Demo actor의 password, identifier, email, OAuth link, role와 account lifecycle 변경은 showcase policy에서 금지한다. 일반 회원용 use case 자체를 제거하지 않고 showcase actor에 대해서만 거부한다.
- Public signup과 Kakao·Naver OAuth entry는 showcase profile에서 비활성화하지만 provider stub을 사용한 integration·E2E test로 실제 flow를 검증한다.
- Demo actor가 소유하거나 생성한 mutable data는 매일 정해진 maintenance window에 versioned seed 상태로 복구한다. 사용자 생성 media와 파생 projection도 함께 cleanup·reconcile한다.
- Reset은 전체 database truncate나 production backup overwrite를 사용하지 않고 demo actor scope를 확인하는 idempotent operations job으로 실행한다.
- Seed manifest와 checksum으로 고정 fixture drift를 탐지하고 reset 결과, 삭제 row·object 수, projection reconciliation과 실패를 audit·metric으로 남긴다.
- 동시 사용자가 같은 demo 계정 상태를 변경할 수 있다는 제한, reset 시각과 데이터 만료를 화면에 명시한다.
- Portfolio 전용 tenant column, per-request schema, copy-on-write dataset과 lease lifecycle을 domain model에 추가하지 않는다.

### Alternatives

- 방문자별 DemoLease와 임시 actor: 격리는 좋지만 showcase 전용 provisioning·cleanup과 상태가 추가된다.
- 하나의 공용 계정: 가장 단순하지만 동시 충돌과 scenario 훼손 가능성이 여러 계정보다 크다.
- 공개 회원가입: 계정 충돌은 줄지만 실제 개인정보 처리와 abuse·탈퇴 운영 범위를 만든다.
- Frontend-only role simulation: 안전하지만 실제 Spring Security·session·authorization 동작을 증명하지 못한다.

### Consequences

- Showcase가 실제 credentials authentication과 domain use case를 사용해 구현과 설명이 단순하다.
- 같은 계정의 동시 사용자가 서로의 변경을 보고 optimistic conflict를 경험할 수 있으며 이를 알려야 한다.
- 공개 credential은 공격자가 알 수 있으므로 rate limit, mutation scope, upload quota와 reset automation이 필수다.
- Demo actor 예외가 domain code에 흩어지지 않도록 showcase authorization policy 또는 actor classification 한 곳에서 관리해야 한다.
- Reset 작업 자체가 scoped maintenance, idempotency, reconciliation과 운영 관측의 포트폴리오 증거가 된다.

### Evidence

- ADR-0029: 공개 배포는 합성 데이터와 제한된 demo actor를 사용하는 portfolio sandbox다.
- ADR-0009: Browser authentication은 Spring Security와 PostgreSQL-backed server session을 사용한다.
- 사용자 결정(2026-08-10): 방문자별 lease가 아니라 일반 고정 계정으로 showcase를 체험하게 한다.
- 사용자 승인(2026-08-10): 여러 MEMBER 계정, 제한된 MODERATOR 계정과 일일 scoped seed 복구 방식을 채택했다.

### Open Questions

- 정확한 demo persona, 초기 상태와 계정별 parity scenario를 fixture specification에서 정의해야 한다.
- Daily reset maintenance window와 reset 중 write 처리 방식을 정해야 한다.
- 공개 moderator가 수행할 수 있는 reversible action의 정확한 범위를 정해야 한다.

## ADR-0031 - Marketplace를 결제 없는 반려용품 Classified Listing으로 한정한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet의 `MarketListing`은 `Post`와 1:1로 연결되고 판매, 대여, 나눔 유형과 가격·상품 상태·보증금·기간·거래 상태를 표현한다. 작성자와 moderator가 상태를 바꿀 수 있지만 구매자, 주문, 결제, 배송, 정산, 채팅과 거래 계약 model은 없다. 거래 안전 안내도 TownPet이 거래 당사자가 아님을 명시한다. 이를 Spring으로 옮기면서 실제 commerce platform처럼 확장하면 결제·분쟁·개인정보·보안 범위가 크게 늘고 기존 UI·동작과 이질감이 생긴다.

### Decision

Marketplace bounded context는 반려용품의 지역 판매·대여·나눔 정보를 게시하고 거래 가능 상태를 알리는 classified listing으로 한정한다.

- TownPet은 판매자와 관심 사용자가 공개 게시물·댓글로 정보를 교환하도록 돕지만 거래 계약의 당사자, 결제대행자, 에스크로·배송·정산·환불 주체가 아니다.
- 이번 제품 범위에 주문, payment, escrow, shipment, settlement, refund, private chat와 전화번호 중개 model을 추가하지 않는다.
- `MarketListing`은 주문이 아니라 작성자가 소유한 listing aggregate이며 `publicationId`, listing type, price, condition, deposit, period와 availability lifecycle을 관리한다.
- Domain lifecycle은 `AVAILABLE`, `RESERVED`, `COMPLETED`, `CANCELLED`를 사용한다. 판매뿐 아니라 대여·나눔에도 의미가 맞지 않는 `SOLD`는 domain 용어로 사용하지 않는다.
- Legacy contract·fixture의 `SOLD` 값이 parity 기간에 필요하면 transport·migration anti-corruption mapping에서 `COMPLETED`로 변환하고 domain model에 누출하지 않는다.
- 임의 `setStatus` command 대신 `reserve`, `reopen`, `complete`, `cancel`처럼 의도를 드러내는 transition command를 제공한다.
- 정상 transition은 `AVAILABLE → RESERVED|COMPLETED|CANCELLED`, `RESERVED → AVAILABLE|COMPLETED|CANCELLED`이며 `COMPLETED`와 `CANCELLED`는 작성자에게 terminal state다.
- Listing 작성자만 정상 business transition을 수행하고 optimistic version으로 동시 변경을 탐지한다. 같은 idempotency key의 재요청은 기존 결과를 반환한다.
- Moderator는 business availability를 임의 변경하지 않고 TrustSafety의 visibility restriction·report workflow를 사용한다.
- 데이터 오류 복구가 필요한 경우 Operator의 별도 repair command가 사유, before·after, actor와 audit를 남기고 policy를 우회한 사실을 명시한다.
- 상태 변경은 `MarketListingReserved`, `MarketListingReopened`, `MarketListingCompleted`, `MarketListingCancelled` event를 발행해 feed, search, notification과 audit projection을 갱신한다.
- 완료·취소 listing의 publication을 자동 삭제하지 않는다. 기존 사용자 경험과 정책에 따라 상세는 유지하고 feed·search에서 상태 filter·badge를 제공한다.
- Marketplace 신고·금지 품목 판정은 availability lifecycle과 분리된 TrustSafety restriction으로 처리한다.

### Alternatives

- 주문·결제 marketplace로 확장: 더 복잡한 backend를 보여줄 수 있지만 기존 제품 경계를 바꾸고 결제·환불·정산·법적 책임을 크게 늘린다.
- Private chat만 추가: 거래 조율은 편해지지만 메시지 privacy, abuse, retention과 실시간 delivery라는 별도 bounded context가 필요하다.
- 현재 `MarketStatus`를 그대로 임의 변경: parity는 쉽지만 command 의도, terminal invariant와 concurrency conflict가 드러나지 않는다.
- Moderator의 모든 상태 override 유지: 운영 편의는 있지만 business lifecycle과 content moderation 권한을 혼합한다.

### Consequences

- TownPet의 기존 거래 UI·안내와 일치하면서도 명확한 aggregate·state machine·event를 구현할 수 있다.
- 실제 거래 상대, 결제 성공, 배송과 분쟁 결과는 TownPet이 검증하거나 보증할 수 없다.
- 댓글이 공개 조율 수단이므로 전화번호·외부 연락처 노출 방지와 신고 안내가 중요하다.
- Legacy `SOLD`와 domain `COMPLETED` 사이 mapping과 differential normalization이 필요하다.
- Terminal 상태를 작성자가 되돌릴 수 없으므로 오조작 복구는 Operator workflow 또는 새 listing 생성으로 처리된다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: `MarketListing` 1:1 `Post`, `SELL|RENT|SHARE`, 가격·상태·보증금·기간과 `AVAILABLE|RESERVED|SOLD|CANCELLED`를 정의한다.
- `/Users/alex/project/townpet/app/src/server/services/posts/post-market-workflow.service.ts`: 작성자 transition과 moderator override의 현재 동작을 정의한다.
- `/Users/alex/project/townpet/app/src/lib/guide-pages.ts`: TownPet이 중고용품 정보 공유를 돕지만 거래 당사자가 아님을 안내한다.
- 사용자 승인(2026-08-10): 결제·채팅 없는 classified listing 경계와 의도 기반 상태 머신을 채택했다.

### Open Questions

- SELL, RENT, SHARE별 price, deposit와 period 불변식을 확정해야 한다.
- 공개 댓글에서 개인정보·외부 연락 유도를 제한할 정확한 policy와 detection 방식을 정해야 한다.
- Terminal 상태 오조작 시 사용자용 correction request와 Operator repair 중 UX를 선택해야 한다.

## ADR-0032 - 거래 유형별 Sealed Terms와 Database Constraint로 잘못된 조합을 막는다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Legacy `MarketListing`은 `listingType`, `price`, nullable `depositAmount`, nullable `rentalPeriod`를 한 구조로 저장하고 validation은 가격·보증금의 음수와 기간 길이만 검사한다. UI는 나눔 가격을 0원으로 안내하지만 backend는 유형별 조합을 강제하지 않아 판매 0원, 나눔 보증금, 대여 기간 누락 같은 의미 없는 상태가 가능하다. Spring domain model에서 같은 nullable bag을 복제하면 aggregate가 항상 유효하다는 보장을 잃는다.

### Decision

Marketplace는 유형별 sealed `ListingTerms` value object와 동일한 PostgreSQL check constraint를 사용해 생성·수정·migration 후 항상 유효한 거래 조건만 허용한다.

- `ListingTerms`는 `SaleTerms`, `RentalTerms`, `ShareTerms`로만 구현 가능한 Java sealed interface다.
- `SaleTerms`는 1원 이상 1억 원 이하의 KRW 판매 가격을 필수로 가지며 보증금과 대여 기간을 가지지 않는다.
- `RentalTerms`는 0원 이상 1억 원 이하의 KRW 대여료, 선택적 0원 이상 1억 원 이하 보증금과 trim 후 1자 이상 80자 이하의 기간 설명을 가진다.
- `ShareTerms`는 별도 금액·보증금·대여 기간을 가지지 않고 transport·storage projection에서 가격을 0원으로 표현한다.
- 공통 상품 상태 `NEW`, `LIKE_NEW`, `GOOD`, `FAIR`는 필수 value이며 unknown legacy value는 암묵적으로 `GOOD`으로 바꾸지 않는다.
- PostgreSQL은 단일 `market_listing` table을 유지하되 `listing_type`, price, deposit와 period의 허용 조합을 `CHECK` constraint로 강제한다.
- OpenAPI request는 listing type discriminator가 있는 `oneOf` schema로 유형별 required·forbidden field를 표현하고 generated TypeScript client도 discriminated union을 사용한다.
- Money parsing은 integer KRW만 허용하고 decimal, exponent, overflow, negative와 upper-bound 초과를 transport와 domain에서 거부한다.
- 거래 조건과 listing type은 `AVAILABLE`에서만 변경할 수 있다. `RESERVED` 이후에는 type, price, condition, deposit와 period를 변경할 수 없다.
- 예약 상태의 조건을 고치려면 작성자가 `reopen`해 `AVAILABLE`로 전환한 뒤 version을 포함해 수정한다. `COMPLETED`, `CANCELLED`의 조건은 사용자 명령으로 변경하지 않는다.
- Condition 변경과 lifecycle transition은 aggregate version을 증가시키며 충돌한 stale command는 `409` ProblemDetail로 처리한다.
- Rental period는 당사자 간 실제 예약 계약이 아니라 게시 정보이므로 구조화 날짜·availability calendar로 확장하지 않고 bounded description으로 유지한다.
- Migration은 legacy 조합을 유형별 규칙으로 분류해 자동 변환, 명시적 default 또는 quarantine으로 나누고 원본 ID·이유·변환 결과를 reconciliation report에 남긴다.
- 의미가 명백하지 않은 legacy row를 추정으로 수정하지 않고 공개 cutover 전에 fixture owner 또는 운영 repair decision을 요구한다.

### Alternatives

- 기존 nullable 구조를 Java record 하나로 복제: mapping은 단순하지만 잘못된 field 조합을 모든 호출부에서 반복 검사해야 한다.
- 유형별 table 상속: database null은 줄지만 작은 공통 lifecycle에 여러 table·join과 migration 복잡성이 추가된다.
- 대여를 booking aggregate로 확장: 날짜 충돌과 예약을 모델링할 수 있지만 TownPet이 거래 당사자가 아닌 classified 경계를 넘는다.
- Validation은 application에만 구현: 정상 API는 보호하지만 migration, repair와 직접 SQL이 무효 상태를 만들 수 있다.

### Consequences

- Compiler와 database가 유형별 invariant를 함께 보장하고 OpenAPI client도 잘못된 payload를 만들기 어렵다.
- JPA single table mapping과 sealed domain mapping 사이 변환 코드가 필요하다.
- Reserved 후 조건 수정이 제한되어 기존보다 명확한 reopen UX와 conflict 안내가 필요하다.
- Legacy invalid row를 발견하면 migration이 자동 성공하지 않고 quarantine·대사 작업이 생길 수 있다.
- 금액 상한 변경은 domain, OpenAPI와 database constraint migration을 함께 검토해야 한다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: 모든 거래 유형이 `price`, nullable `depositAmount`, nullable `rentalPeriod`를 공유한다.
- `/Users/alex/project/townpet/app/src/lib/validations/posts/post.ts`: price·deposit의 최소값과 period 길이만 검사하고 유형별 조합은 검사하지 않는다.
- `/Users/alex/project/townpet/app/src/components/posts/post-create-submit.ts`: 나눔은 0원을 입력하라는 UI 안내만 제공한다.
- 사용자 승인(2026-08-10): sealed 유형별 terms, DB check, 상태별 수정 제한과 legacy quarantine 전략을 채택했다.

### Open Questions

- Legacy RENT의 빈 기간과 SHARE의 비영 가격을 자동 보정할 정확한 migration rule을 inventory 후 확정해야 한다.
- `condition` 외에 상품 category·quantity가 parity 범위에 필요한지 기존 data 분포로 확인해야 한다.
- Reopen 이후 이전 reservation 의미를 audit에 어떻게 표시할지 event payload를 정의해야 한다.

## ADR-0033 - Marketplace 금지 품목은 Versioned Hard Rule과 Soft Signal로 판정한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

기존 TownPet은 정규식으로 동물 생체 판매·분양, 유통기한이 지난 사료·간식과 동물 의약품 표현을 탐지해 거래 글 생성을 막는다. 반려동물 특화 safety intent는 분명하지만 단일 정규식은 공백·Unicode 변형으로 우회할 수 있고 `책임비`, 개봉 식품, 외부 연락·선입금 같은 문맥은 오탐 없이 hard block하기 어렵다. 모든 의심 신호를 자동 삭제하면 정상 게시물을 막고, 반대로 안내문만 두면 금지 품목이 여러 생성 경로로 유입된다.

### Decision

Marketplace는 versioned deterministic policy를 모든 create·edit 경로에 적용하고 high-confidence hard block과 review 가능한 soft signal을 분리한다.

- Title, body와 거래 조건의 검사 대상 text를 Unicode NFKC, case, 반복 공백·구분자와 알려진 자모 우회 규칙으로 정규화하되 원문을 policy log에 복사하지 않는다.
- 동물 생체 판매·유상 분양, 유통기한 경과·폐기 대상 사료·간식과 처방약·동물 의약품 거래의 high-confidence 표현은 creation·edit을 hard block한다.
- 책임비, 개봉 식품, 외부 messenger·선입금 유도, 반복 전화번호·계좌번호와 비정상 고가 표현은 자동 삭제 근거가 아니라 warning과 `AbuseSignal`로 기록한다.
- Hard block은 stable reason code, rule version과 사용자가 내용을 고칠 수 있는 한국어 설명을 반환한다. 내부 detection pattern 전체는 공격자에게 노출하지 않는다.
- 계좌·전화번호·email과 민감 원문은 metric, audit, trace에 저장하지 않고 signal category, rule ID, actor, target과 decision만 기록한다.
- Policy rule과 normalization은 version-controlled data·code로 관리하고 각 rule에 positive, negative, obfuscation과 known false-positive corpus test를 둔다.
- 새 rule은 기존 fixture·anonymized corpus에 shadow evaluation해 예상 block·warning 변화와 오탐을 review한 뒤 hard enforcement로 승격한다.
- LLM·외부 AI moderation 결과는 hard block, visibility restriction과 sanction의 단독 근거로 사용하지 않는다.
- Deterministic 판단이 모호하면 게시를 허용하되 사용자 경고, report 진입과 moderator review signal을 제공한다.
- 일반 post 작성, guest 작성, edit, breed lounge group-buy와 future import가 모두 같은 Marketplace application policy를 호출한다. Transport별 정규식 복제를 금지한다.
- 이미 게시된 listing에서 위반이 발견되면 Marketplace가 publication row를 직접 숨기지 않고 TrustSafety에 evidence reference와 restriction request를 전달한다.
- 작성자가 내용을 수정해 policy를 통과하면 정상 create·edit flow를 다시 수행할 수 있고 반복 위반은 TrustSafety의 account·guest sanction input이 된다.
- Rule별 block 수, soft-signal 수, 수정 후 성공률, appeal·moderator overturn과 false-positive corpus를 관측해 policy 품질을 review한다.
- Showcase UI에는 실제 개인정보, 계좌와 외부 연락처를 입력하지 말아야 함을 작성·댓글 지점에 명시한다.

### Alternatives

- 정규식 hard block만 유지: 단순하지만 우회·오탐·rule version과 판정 근거를 운영하기 어렵다.
- 모든 의심 표현 hard block: 안전해 보이지만 정상 문맥을 과도하게 막고 사용자 수정 가능성이 낮다.
- LLM moderation 자동 삭제: 문맥 이해 가능성은 있지만 비결정성, 비용, 개인정보 전송과 설명·재현 문제가 있다.
- 사후 신고만 사용: 오탐은 적지만 명백한 금지 품목이 공개된 뒤에야 대응한다.

### Consequences

- 명백한 금지 품목을 일관되게 막으면서 모호한 위험 신호는 사람 검토와 측정 대상으로 남긴다.
- Normalization·corpus·shadow evaluation과 rule lifecycle을 유지하는 운영 비용이 생긴다.
- 탐지하지 못한 우회 표현이 존재할 수 있으므로 신고·moderation과 반복 위반 제재가 계속 필요하다.
- Privacy-safe signal만 보관하므로 원문 재현이 필요한 moderation evidence는 접근 통제된 publication snapshot 정책과 연결해야 한다.
- Marketplace와 TrustSafety 사이 restriction request·evidence contract가 필요하다.

### Evidence

- `/Users/alex/project/townpet/app/src/lib/market-safety-policy.ts`: 세 금지 범주의 현재 정규식과 안전 checklist를 정의한다.
- `/Users/alex/project/townpet/app/src/lib/market-safety-policy.test.ts`: 금지 표현과 사용자 메시지의 현재 regression 사례다.
- `/Users/alex/project/townpet/app/src/lib/validations/posts/post.ts`: create validation에서 marketplace safety policy를 호출한다.
- 사용자 승인(2026-08-10): deterministic hard block, soft abuse signal, corpus·shadow evaluation과 TrustSafety restriction 경계를 채택했다.

### Open Questions

- Initial Korean normalization corpus와 false-positive fixture를 실제 legacy data inspection으로 구성해야 한다.
- 어떤 soft signal 조합을 moderator queue 우선순위로 승격할지 threshold를 측정 후 정해야 한다.
- 공개 댓글의 연락처·계좌 노출을 Marketplace rule과 공통 interaction policy 중 어디서 소유할지 결정해야 한다.

## ADR-0034 - Care를 결제 없는 이웃 간 돌봄 Coordination Workflow로 한정한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Legacy Care는 산책, 급식, 방문 돌봄, 병원 동행, 긴급 확인과 심부름 요청에 여러 회원이 지원하고 한 명을 수락해 진행·완료하며 양측 feedback을 받는다. 보상 금액과 결제·사기 issue type은 있지만 payment, settlement, 신원·자격 검증, 보험과 손해배상 model은 없다. 이를 전문 펫시터 marketplace처럼 표현하면 실제로 제공하지 않는 안전·결제 보장을 암시하고 민감한 주소·출입·반려동물 건강 정보 처리 범위가 커진다.

### Decision

Care bounded context는 이웃 간 반려동물 돌봄 요청, 지원, 한 명의 매칭, 진행·완료와 비공개 안전 feedback을 조율하는 workflow로 한정한다.

- TownPet은 요청자와 돌봄 제공자의 고용주, 전문 펫시터 인증기관, 결제·정산·보험·손해배상 중개자가 아니다.
- `rewardAmount`는 작성자가 제시한 원 단위 참고 정보이며 payment intent, 지급 약속, settlement status와 platform fee를 만들지 않는다.
- 결제, 환불, 수수료, 세금, 보험 claim과 손해배상 workflow를 이번 scope에 추가하지 않는다.
- TownPet이 검증하지 않은 신원, 자격, 경력과 보험에 verified badge를 표시하지 않는다. Member profile reputation을 safety guarantee로 표현하지 않는다.
- 병원 동행은 이동·보호자 보조 범위이며 진료 동의 대행, 처방 판단, 투약과 의료행위 위임을 지원하지 않는다.
- `EMERGENCY_CHECK`는 이웃의 현장 확인 요청으로 표현하고 응급의료·구조·경찰·소방을 대체하지 않는 즉시 연락 안내를 함께 제공한다.
- 정확한 주소, 공동현관·도어락 code, 전화번호, 건강·처방 정보와 금융정보는 public publication field와 search projection에 저장하지 않는다.
- Care application message는 요청자, 해당 applicant와 권한 있는 safety reviewer만 읽을 수 있고 public post·feed·search response에 포함하지 않는다.
- 구체적 민감 정보 교환을 위한 private chat은 현재 scope에 없으므로 public text에 입력하지 말라는 안내·detection을 제공하고 실제 돌봄 계약 완결을 보장하지 않는다.
- Completion feedback은 공개 rating·review가 아니라 참여자와 TrustSafety가 접근하는 비공개 outcome·safety signal이다.
- Care issue feedback은 account sanction을 직접 실행하지 않고 evidence reference와 함께 TrustSafety review를 요청한다.
- Showcase에서는 합성 요청·지원·매칭·완료 scenario로만 체험하고 실제 돌봄 모집이나 실제 주소·보상 입력을 금지한다.
- Product copy, OpenAPI와 README에서 이 coordination 경계와 emergency·medical 한계를 일관되게 표시한다.

### Alternatives

- 전문 pet-sitting marketplace: 신뢰와 수익 model은 커지지만 identity verification, payment, insurance, dispute와 법적 운영이 필수다.
- Care를 일반 게시글로 축소: 안전 범위는 단순하지만 이미 존재하는 지원·단일 매칭·진행·feedback workflow를 잃는다.
- 공개 별점·후기 도입: 선택 정보는 늘지만 보복, 명예·분쟁, moderation과 낮은 표본의 reputation 왜곡이 생긴다.
- Private chat 추가: 민감정보 교환은 쉬워지지만 message privacy, abuse, retention과 실시간 delivery라는 새 범위가 필요하다.

### Consequences

- 기존 Care의 상태·지원·feedback 깊이는 유지하면서 제공하지 않는 결제·전문성 보장을 피한다.
- 실제 돌봄을 끝까지 조율할 private communication 수단이 없으므로 공개 community 기능의 한계를 명시해야 한다.
- Application message와 feedback에 별도의 authorization, encryption·retention과 leakage test가 필요하다.
- Emergency·병원 동행 문구는 사용자가 전문 서비스를 오인하지 않도록 UX parity와 함께 안전 문구를 보완해야 한다.
- 향후 결제·신원 검증·chat을 추가하려면 별도 bounded context와 readiness decision이 필요하다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: `CareRequest`, `CareApplication`, `CareCompletionFeedback`, reward와 safety issue type을 정의한다.
- `/Users/alex/project/townpet/app/src/server/services/posts/post-care-workflow.service.ts`: 지원, 단일 수락, 요청 상태 전이와 비공개 feedback 동작을 구현한다.
- `/Users/alex/project/townpet/app/e2e/care-request-flow.spec.ts`: 요청자·지원자 간 실제 UI workflow의 parity 근거다.
- 사용자 승인(2026-08-10): 결제·전문 자격 보장 없는 care coordination 경계와 비공개 feedback 정책을 채택했다.

### Open Questions

- 요청·지원·매칭·취소·완료의 aggregate 경계와 양측 transition authority를 확정해야 한다.
- Public text의 주소·출입·의료·연락 정보 detection과 safe correction UX를 정의해야 한다.
- Application·feedback의 retention과 field-level 보호 수준을 showcase·future launch별로 결정해야 한다.

## ADR-0035 - Care의 모집과 수행을 Request·Application·Assignment로 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Legacy는 `CareRequest.status` 하나로 모집, 매칭, 수행과 완료를 표현하고 `CareApplication`의 수락 상태를 함께 갱신한다. 지원자는 pending일 때만 철회할 수 있고, 수락 후 철회·요청자 취소·진행 중 중단이 application 상태와 일치하지 않을 수 있다. 동시 수락을 database constraint로 막는 active assignment도 없다. 다만 사용자는 Care가 포트폴리오의 핵심 우선순위는 아니므로 기본 무결성을 넘는 세부 기능 확장은 원하지 않는다.

### Decision

Care의 기본 무결성을 위해 모집, 후보 지원과 실제 수행을 `CareRequest`, `CareApplication`, `CareAssignment`로 분리하되 추가 기능은 parity 범위에 제한한다.

- `CareRequest`는 요청 정보와 `OPEN`, `MATCHED`, `CANCELLED`, `EXPIRED` 모집 lifecycle을 소유한다.
- `CareApplication`은 요청별 지원자의 `PENDING`, `ACCEPTED`, `DECLINED`, `WITHDRAWN` 후보 lifecycle을 소유한다.
- `CareAssignment`는 지원 수락 시 생성되며 requester, caregiver와 source application을 참조하고 `MATCHED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED_BY_REQUESTER`, `CANCELLED_BY_CAREGIVER`, `ABORTED` 수행 lifecycle을 소유한다.
- 한 사용자는 같은 요청에 한 번만 지원하고 자기 요청에는 지원할 수 없으며 `OPEN` 요청에만 새 지원·수락을 허용한다.
- 지원 수락은 request의 `OPEN → MATCHED` 조건부 update, 선택 application 수락, assignment 생성과 나머지 pending 지원 거절을 한 transaction에서 수행한다.
- PostgreSQL partial unique index로 요청당 active assignment를 최대 하나만 허용하고 concurrent acceptance test를 둔다.
- Pending 지원자는 철회할 수 있다. 수락된 caregiver가 시작 전에 철회하면 assignment를 취소하고 아직 유효한 request는 `OPEN`으로 되돌릴 수 있다.
- Requester가 시작 전에 취소하면 request와 active assignment를 함께 취소한다. 시작 후 중단은 `ABORTED`와 사유로 기록한다.
- Requester와 caregiver 모두 기존 UI처럼 시작·완료 command를 실행할 수 있고 actor·time·previous state를 event와 audit에 기록한다.
- 화면의 effective care status는 request와 assignment를 조합해 기존 모집 중, 매칭됨, 진행 중, 완료, 취소 표현을 유지한다.
- Feedback, 상세 취소 UX, reward와 privacy 세부는 추가 product 확장 없이 legacy parity와 공통 authorization·TrustSafety 정책을 따른다.
- Care의 후속 세부 modeling은 구현 중 실제 parity blocker나 무결성 결함이 발견될 때만 ADR을 추가한다.

### Alternatives

- Legacy 단일 status 유지: 구현은 빠르지만 지원 수락과 수행 lifecycle 불일치를 database에서 막기 어렵다.
- Care를 전문 marketplace 수준으로 확장: 세밀한 계약·정산·분쟁을 다룰 수 있지만 사용자 우선순위와 범위를 넘는다.
- Care 기능 제거: 범위는 줄지만 기존 TownPet parity를 깨고 이미 존재하는 E2E flow를 잃는다.

### Consequences

- 단일 active match와 취소·중단 일관성을 보장하면서 Care 기능 확장을 최소화한다.
- Aggregate와 table이 하나 늘고 legacy status를 effective status로 변환해야 한다.
- 세부 feedback·운영 정책은 공통 정책과 기존 behavior에 의존하며 독립적인 고도화는 하지 않는다.
- Care 구현은 핵심 community·lostfound·marketplace domain보다 낮은 우선순위 phase에 배치할 수 있다.

### Evidence

- `/Users/alex/project/townpet/app/src/server/services/posts/post-care-workflow.service.ts`: 수락 시 request와 applications를 갱신하는 현재 transaction 및 수락 후 철회 공백의 근거다.
- `/Users/alex/project/townpet/app/src/server/services/posts/care-workflow-policy.ts`: 요청자·수락 지원자의 현재 상태 전이 규칙이다.
- 사용자 승인(2026-08-10): Request·Application·Assignment 분리와 단일 active assignment 무결성을 채택했다.
- 사용자 우선순위(2026-08-10): Care의 추가 grilling은 생략하고 더 빠르고 느슨하게 parity 중심으로 진행한다.

### Open Questions

- 세부 사항은 선행 설계 질문으로 유지하지 않고 implementation parity matrix와 test에서 발견되는 blocker에 한해 처리한다.

## ADR-0036 - 구조화된 SightingReport를 일반 Comment에서 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Legacy는 일반 `Comment` table에 `kind`, sighting location·time·image와 private flag nullable column을 추가해 목격 제보를 표현한다. 목격 제보는 LostFound 게시물에만 생성되고 위치·시간이 필수이며 보호자 전용 공개, 별도 관리 화면과 privacy validation을 가진다. 일반 댓글의 답글·reaction·수정 lifecycle과 구조·권한이 다른데 같은 aggregate로 처리하면 nullable 상태, 정보 누출과 module ownership 혼선이 생긴다.

### Decision

LostFound bounded context가 `LostFoundAlert`와 별도 `SightingReport` aggregate를 소유하고 Engagement의 일반 `Comment`는 공개 대화만 담당한다.

- `LostFoundAlert`는 publication ID, alert type, pet description, 마지막 확인 시각·위치와 alert lifecycle을 소유한다.
- `SightingReport`는 alert ID, reporter principal, observed time, location disclosure, description, media reference와 report lifecycle을 소유한다.
- SightingReport 생성은 active LostFoundAlert에서만 허용하고 위치·시간·설명을 domain invariant로 검증한다.
- Member와 GuestPrincipal 모두 report할 수 있으며 guest 관리 credential·abuse signal 정책을 그대로 적용한다.
- 일반 `Comment`에서 sighting kind와 sighting-specific nullable column을 제거하고 reply·reaction 등 engagement 기능은 일반 댓글에만 유지한다.
- Public SightingReport는 기존 화면에서 댓글 카드와 같은 visual component로 projection할 수 있지만 API type과 domain identity는 Comment와 구분한다.
- Owner-only report의 위치·사진·민감 설명은 public post, comment, feed, search, share·poster, notification과 telemetry payload에 포함하지 않는다.
- Owner-only 원문은 alert owner와 제한된 TrustSafety reviewer만 policy를 통과해 읽을 수 있고 조회 자체를 audit한다.
- SightingReport는 제출자 수정·철회, owner 확인과 TrustSafety restriction을 표현하는 자체 lifecycle·version을 가진다. Publication·Comment 삭제 상태를 재사용하지 않는다.
- Legacy `CommentKind.LOST_FOUND_SIGHTING` row는 ETL로 SightingReport에 변환하고 source comment ID mapping을 보존한다.
- Differential normalization은 legacy sighting comment와 신규 SightingReport projection을 같은 사용자 의미로 비교해 UI·API parity를 유지한다.
- Migration 완료 후 신규 code는 sighting을 Comment repository로 생성·변경하지 못하게 module verification과 architecture test로 막는다.

### Alternatives

- Legacy Comment subtype 유지: table 수는 적지만 nullable field, 특수 validation과 private authorization이 Engagement에 계속 섞인다.
- 모든 sighting을 private message로 처리: privacy는 강화되지만 공개 목격 정보 공유와 기존 댓글형 UX를 잃는다.
- SightingReport와 Comment를 JPA inheritance로 연결: object hierarchy는 만들 수 있지만 서로 다른 lifecycle·module owner와 table coupling이 남는다.

### Consequences

- 목격 위치·사진 privacy를 일반 comment 응답과 구조적으로 분리하고 LostFound가 규칙을 소유한다.
- 공개 화면에서 Comment와 SightingReport를 시간순으로 합치는 read projection과 cursor 규칙이 필요하다.
- 기존 comment ID를 참조한 신고·notification·analytics data를 mapping·대사해야 한다.
- SightingReport용 수정·철회·restriction·audit API와 test가 별도로 필요하다.
- UI는 동일하게 보일 수 있지만 generated TypeScript type과 client event handling은 두 resource를 구분한다.

### Evidence

- `/Users/alex/project/townpet/app/prisma/schema.prisma`: `Comment`에 `CommentKind.LOST_FOUND_SIGHTING`과 위치·시간·사진·private field가 함께 존재한다.
- `/Users/alex/project/townpet/app/src/lib/validations/comment.ts`: Sighting comment에 위치·시간과 privacy 규칙을 별도로 적용한다.
- `/Users/alex/project/townpet/app/src/server/queries/lost-found-sighting-management.queries.ts`: owner·moderator 전용 sighting 관리 query가 일반 comment와 다른 접근을 이미 요구한다.
- 사용자 승인(2026-08-10): SightingReport aggregate 분리와 legacy projection parity 방식을 채택했다.

### Open Questions

- Public approximate location과 owner-only exact location의 저장·공개 정밀도를 확정해야 한다.
- SightingReport lifecycle의 수정·철회·owner 확인 상태와 보존 기간을 결정해야 한다.
- Comment와 SightingReport를 합치는 cursor의 안정된 ordering key를 정의해야 한다.

## ADR-0037 - PostGIS 공개 근사 위치와 암호화된 보호자 전용 정확 위치를 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

LostFound는 근처의 active alert와 sighting을 찾으려면 위치 검색이 필요하지만 정확 좌표·상세 주소를 공개하면 reporter 또는 보호자의 생활 위치가 노출될 수 있다. Legacy는 alert에 nullable `lat`, `lng` double과 public location text를 두고, sighting comment에는 private flag와 위치 text를 저장한다. 두 double column은 좌표계·유효성·거리 index를 보장하지 않고 private flag 하나는 public projection에 exact field가 섞이는 실수를 구조적으로 막지 못한다.

### Decision

PostgreSQL 18에 PostGIS 3.6 extension을 사용하고 public approximate geography와 owner-only exact location을 별도 표현·보호한다.

- Spatial field는 독립 `lat`·`lng`가 아니라 WGS84 `geography(Point, 4326)`와 명시적 domain `LocationEvidence` value object를 사용한다.
- Public location은 입력 지점에서 약 250m precision으로 낮춘 approximate point와 동네·공원·역·상가·교차로 수준의 safe display label로 구성한다.
- 상세 주거 주소, 동·호수, 공동현관·도어락 정보, 전화번호와 직접 연락 identifier는 public location·description에 허용하지 않는다.
- Exact point와 민감 location text는 필요한 sighting에만 받고 application-level authenticated encryption으로 별도 저장한다. Ciphertext와 key version은 저장하되 key는 database·image·backup과 분리한다.
- Exact location은 spatial index, feed, search, analytics, share·poster, notification과 public API projection에 포함하지 않는다.
- SightingReport disclosure는 `PUBLIC_APPROXIMATE` 또는 `OWNER_ONLY_EXACT`다. 두 경우 모두 public projection은 exact field를 type 수준에서 가지지 않는다.
- `PUBLIC_APPROXIMATE` report는 안전한 설명·사진·observed time과 approximate location만 공개한다. Exact evidence가 있다면 reporter, alert owner와 정책상 reviewer만 접근한다.
- `OWNER_ONLY_EXACT` report는 공개 화면에 제보 존재와 보호자 전용 안내만 표시하고 위치·사진·민감 설명은 alert owner와 case scope가 있는 TrustSafety reviewer만 읽는다.
- Exact evidence 조회는 actor, purpose, report, result와 timestamp를 append-only audit하고 bulk export를 허용하지 않는다.
- Active alert의 반경 검색과 지도·feed candidate는 approximate geography만 대상으로 `ST_DWithin`과 GiST index를 사용한다.
- `RESOLVED`·`CLOSED` alert는 active proximity index query에서 즉시 제외하되 history·owner management policy에 따라 record를 유지한다.
- Domain은 PostGIS·encryption type을 모르고 persistence adapter가 value object와 geography·ciphertext mapping을 담당한다. Spatial read query는 jOOQ로 구현한다.
- Local·CI는 production과 같은 PostGIS-enabled PostgreSQL Testcontainers를 사용해 SRID, invalid coordinate, precision reduction, GiST plan과 private projection leakage를 검증한다.
- Legacy `lat`·`lng`는 range·finite·pair completeness를 검사해 geography로 변환한다. Text label·neighborhood와 명백히 모순되거나 단일 coordinate만 있는 row는 quarantine한다.
- Showcase fixture는 실제 거주지와 무관한 합성 좌표만 사용한다.

### Alternatives

- `double lat/lng` 유지: 단순하지만 좌표계, validity, 거리 단위와 spatial index 사용을 매 query가 책임져야 한다.
- 모든 정확 좌표 공개: 탐색에는 유리하지만 reporter·주거지 노출 위험이 크고 기존 privacy intent에 어긋난다.
- 위치 text만 사용: privacy는 안내로 관리할 수 있지만 반경 검색·지도와 좌표 검증을 구현하기 어렵다.
- Exact point도 PostGIS index에 저장하고 authorization으로만 보호: query는 쉽지만 실수한 projection·운영 query의 노출 blast radius가 커진다.

### Consequences

- 반경 검색 성능과 정확 위치 privacy를 동시에 구조적으로 관리한다.
- PostGIS image, Flyway extension, jOOQ spatial binding, encryption key와 rotation 운영이 추가된다.
- Public approximate point는 실제 목격 지점과 차이가 있으므로 UI에 근사 위치임을 명확히 표시해야 한다.
- Exact evidence는 일반 spatial analytics에 사용할 수 없고 제한된 owner workflow로만 접근한다.
- 250m precision은 초기 정책이며 도시·농촌 UX와 privacy 실측에 따라 versioned policy로 조정해야 한다.

### Evidence

- PostGIS 3.6 공식 배포 정보: PostgreSQL 18 지원 binary를 제공한다.
- PostGIS 공식 `ST_DWithin` 문서: geography의 meter 단위 거리와 spatial index 활용을 설명한다.
- `/Users/alex/project/townpet/app/prisma/schema.prisma`: Legacy alert의 nullable float lat·lng와 comment의 private sighting field를 확인했다.
- `/Users/alex/project/townpet/app/src/lib/lost-found-privacy-policy.ts`: 상세 주소·연락처를 public field에서 막으려는 기존 정책 intent다.
- 사용자 승인(2026-08-10): PostGIS approximate public location과 encrypted owner-only exact evidence 분리를 채택했다.

### Open Questions

- 250m precision의 한국 도시·농촌별 UX를 fixture와 map prototype으로 검증해야 한다.
- Encryption library, KMS가 없는 저비용 환경의 key custody와 rotation 절차를 선택해야 한다.
- Exact evidence와 derived approximate point의 retention·re-derivation 정책을 확정해야 한다.

## ADR-0038 - LostFound 종료 상태에 확인 결과와 종료 사유를 필수로 기록한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Legacy LostFound lifecycle은 `ACTIVE`, `RESOLVED`, `CLOSED`를 제공하고 작성자가 모든 종료 상태에서 다시 active로 전환할 수 있다. 그러나 `RESOLVED`와 `CLOSED`의 의미, 해결 결과, 종료 사유와 재개 이유를 저장하지 않아 사용자 안내·운영 통계·검색 freshness에서 두 상태를 신뢰하기 어렵다. 오래된 alert를 자동 종료하면 실제로 계속 찾는 사건을 숨길 위험도 있다.

### Decision

`RESOLVED`는 확인된 긍정적 결과, `CLOSED`는 해결 확인 없이 제보 접수를 끝낸 상태로 구분하고 모든 종료·재개 command에 구조화된 이유를 요구한다.

- `ACTIVE`는 신규 SightingReport, 반경 검색, active feed와 share를 허용하는 진행 상태다.
- `ACTIVE → RESOLVED`에는 `REUNITED_WITH_GUARDIAN`, `LOCATED_SAFE`, `TRANSFERRED_TO_SHELTER`, `OTHER_RESOLVED` outcome 중 하나와 선택 설명이 필요하다.
- `ACTIVE → CLOSED`에는 `SEARCH_ENDED`, `DUPLICATE`, `INVALID_INFORMATION`, `AUTHOR_REQUEST`, `OTHER_CLOSED` reason 중 하나와 선택 설명이 필요하다.
- `RESOLVED`와 `CLOSED`는 작성자가 이유를 입력해 `ACTIVE`로 재개할 수 있다. 재개하면 이전 outcome·reason을 지우지 않고 status event history에 보존한다.
- `LostFoundStatusEvent`는 actor, from·to state, outcome 또는 reason, bounded note, occurredAt, request·trace ID와 aggregate version을 append-only로 기록한다.
- 작성자만 정상 resolve, close와 reopen command를 수행한다. Moderator는 lifecycle을 대신 수정하지 않고 Publication restriction·report를 사용한다.
- 데이터 오류 정정은 Operator repair command와 before·after audit를 요구한다.
- `RESOLVED`, `CLOSED`에서는 신규 SightingReport를 받지 않지만 기존 public·owner-only report와 status history는 권한에 따라 조회할 수 있다.
- 종료 시 active spatial, feed, notification candidate에서 제거하고 재개 event 처리 시 idempotent하게 복원한다.
- Active alert는 나이만으로 자동 close하지 않는다. 7일, 14일, 30일 상태 확인 알림을 보내고 미확인 기간을 ranking freshness signal로 사용한다.
- 오래된 active alert도 검색에서 삭제하지 않고 last-confirmed age와 active 상태를 명시한다.
- UI label은 기존 `제보 접수 중`, `해결됨`, `종료`를 유지하고 resolve·close dialog에서만 결과·사유를 추가로 받는다.
- Legacy status에는 outcome·reason이 없으므로 active는 그대로 변환하고 resolved·closed는 `LEGACY_UNSPECIFIED` migration marker와 source timestamp를 기록해 사용자 선택 outcome처럼 위조하지 않는다.

### Alternatives

- `RESOLVED`와 `CLOSED`를 하나로 합침: 단순하지만 찾은 경우와 결과 없이 종료한 경우를 구분할 수 없다.
- 기존 상태만 유지: parity는 쉽지만 상태 의미와 운영 지표가 계속 불명확하다.
- 30일 후 자동 close: stale feed는 줄지만 작성자 확인 없이 실제 실종 alert를 종료한다.
- Moderator가 상태를 정리: 데이터는 정돈되지만 content moderation과 사건 당사자의 business outcome을 혼합한다.

### Consequences

- 해결률, shelter transfer와 미해결 종료를 구분해 신뢰 가능한 운영 지표를 만들 수 있다.
- Resolve·close·reopen UI와 API에 추가 field가 필요하지만 기존 status label·화면 흐름은 유지된다.
- Reminder scheduler와 last-confirmed ranking signal을 운영해야 한다.
- Legacy 종료 row는 outcome 분석에서 unknown으로 분리해야 한다.
- 재개 가능한 append-only history가 projection rebuild와 audit의 기준이 된다.

### Evidence

- `/Users/alex/project/townpet/app/src/server/services/posts/post-lost-found-workflow.service.ts`: 현재 상태 전이는 outcome·reason 없이 모든 종료 상태에서 ACTIVE 재개를 허용한다.
- `/Users/alex/project/townpet/app/prisma/schema.prisma`: StatusEvent가 from·to와 actor·time만 저장한다.
- 사용자 승인(2026-08-10): outcome·close reason·reopen reason과 stale reminder 기반 lifecycle을 채택했다.

### Open Questions

- Reminder delivery cadence의 중복·quiet-hours 정책은 Notification 설계에서 확정해야 한다.
- `OTHER_*` note의 privacy 검사와 최대 길이를 OpenAPI contract에 정의해야 한다.
- LostFound의 나머지 상세 UX는 별도 선행 질문 없이 parity matrix와 implementation test에서 처리한다.

## ADR-0039 - 작은 PR과 계층화된 Quality Gate로 설계·자동화·운영 증거를 남긴다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

토스뱅크 Server Developer 인턴십 공고는 Java·Spring·Gradle 경험, 확장 가능한 구조, 요구사항을 데이터 모델·API로 푸는 능력, 성능 최적화, 자동화와 운영 배포 전체 경험을 강조한다. 공고에 등장하는 Kubernetes, Kafka, WebFlux 등의 기술을 단일 VPS portfolio에 억지로 추가하면 문제 규모와 맞지 않고 설명할 수 없는 복잡성만 늘어난다. 반면 test·migration·contract·성능·배포 증거가 자동화되지 않으면 많은 ADR도 실제 품질을 보장하지 못한다.

### Decision

Protected `main`, 작은 pull request와 비용·실행시간에 따라 계층화된 GitHub Actions quality gate를 사용하고 검증된 immutable artifact만 showcase에 배포한다.

- 모든 변경은 목적이 하나인 작은 branch·PR로 제출하고 `main` 직접 push를 금지한다. Solo 개발이어도 review checklist와 CI evidence를 남긴다.
- PR fast gate는 Java 25 Gradle toolchain, Spotless, Error Prone·NullAway, domain unit test, Spring Modulith·ArchUnit verification과 `@ApplicationModuleTest`를 실행한다.
- Repository gate는 PostgreSQL 18 + PostGIS Testcontainers, Flyway empty migration·snapshot upgrade·Hibernate validate, lock·constraint test와 jOOQ generation drift를 검증한다.
- Contract gate는 OpenAPI lint·breaking diff, generated Java transport·TypeScript client drift와 representative API contract test를 실행한다.
- Frontend gate는 TypeScript typecheck, ESLint, Vitest와 핵심 Playwright smoke를 실행한다.
- Security gate는 secret scan, dependency vulnerability, GitHub CodeQL, container build 가능성과 최소 권한 workflow permission을 검사한다.
- Coverage는 전체 숫자 경쟁이 아니라 변경 line 85%, 변경 branch 80%를 기본 gate로 하고 generated code, 단순 configuration 제외 근거를 version control한다.
- Authorization, money, lifecycle, idempotency와 concurrency invariant가 있는 critical domain에는 mutation testing을 적용하고 초기 critical mutation score 80% 이상을 요구한다.
- `main` 병합 후 full gate는 전체 integration·contract, legacy/Spring differential, Playwright dual-target, visual·accessibility, ETL rehearsal와 대표 query plan·performance budget을 실행한다.
- Release workflow는 SBOM, vulnerability report, source commit·dependency·toolchain provenance를 생성하고 GHCR에 immutable image digest를 push한다.
- PR artifact를 public VPS에 배포하지 않는다. 검증된 `main` image만 manual environment approval 후 showcase candidate가 된다.
- Deployment는 backward-compatible Flyway expand migration, 신규 container readiness, API·database·event backlog·browser smoke 후 Caddy upstream을 전환한다.
- 전환 뒤 SLO burn, 5xx, latency와 event backlog를 관찰하고 실패 시 이전 immutable image로 자동 rollback하며 결과를 deployment record에 남긴다.
- Nightly는 full E2E·visual·mutation·controlled performance regression, weekly는 backup restore·projection rebuild, monthly는 dependency·performance baseline review를 수행한다.
- Flaky test는 무제한 retry로 green 처리하지 않고 owner, issue, 최초 발생, 격리 사유와 수정 기한을 기록한다. Critical path flaky test는 release를 막는다.
- GitHub Actions cache는 dependency·generated artifact에만 사용하고 test result 자체를 재사용하지 않는다. Workflow concurrency는 superseded PR run을 취소한다.
- Kubernetes, Kafka, Redis, WebFlux와 별도 CI platform은 실제 bottleneck·delivery requirement와 ADR 없이 pipeline 또는 production에 추가하지 않는다.

### Alternatives

- 모든 검사를 PR마다 실행: feedback은 완전하지만 mutation·dual E2E·migration rehearsal로 개발 iteration이 지나치게 느려진다.
- Unit test와 Docker build만 실행: 빠르지만 schema, module, contract, browser parity와 배포 회귀를 놓친다.
- 직접 main push 후 자동 배포: 단순하지만 실패 artifact와 migration이 공개 환경에 즉시 반영되고 설계 review 증거가 없다.
- 공고 기술 stack을 그대로 모방: 키워드는 늘지만 규모에 맞는 trade-off와 문제 해결 설명이 약해진다.

### Consequences

- Architecture decision이 module, schema, contract, browser, performance와 deployment test로 실제 강제된다.
- CI 시간이 늘어 계층화, change detection과 cache 최적화가 필요하다.
- Mutation·differential·visual baseline을 유지하는 비용이 생기지만 인터뷰에서 개선 과정을 재현할 수 있다.
- Public repository의 PR, check, benchmark와 deployment record가 포트폴리오 evidence가 된다.
- GitHub hosted runner quota와 container image storage를 monitor하고 무료 범위 초과 시 nightly cadence를 조정해야 한다.

### Evidence

- 토스뱅크 인턴십 공고: Java·Spring·Gradle, 확장 가능한 구조, 성능 최적화·자동화, 비즈니스 분석부터 기술 명세·개발·운영 배포까지의 경험을 선호한다고 명시한다.
- ADR-0019: parity matrix, differential·visual·accessibility·performance 검증을 완료 기준으로 채택했다.
- ADR-0021부터 ADR-0025: immutable image 배포, observability, SLO와 Java·Spring 기준선을 정했다.
- 사용자 승인(2026-08-10): 계층화된 PR·main·nightly·weekly quality gate와 evidence 중심 CI/CD를 채택했다.

### Open Questions

- 실제 repository 구성 후 fast gate 목표 시간과 job sharding을 benchmark해 확정해야 한다.
- Mutation 대상 package와 변경 coverage 계산 도구를 scaffold 단계에서 선택해야 한다.
- GitHub Actions 무료 quota 안의 nightly cadence를 최초 4주 사용량으로 재평가해야 한다.

## ADR-0040 - 현재 인증 범위를 Credentials로 한정한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Context

Legacy TownPet에는 Credentials와 Kakao·Naver 인증 및 계정 연결 흐름이 있었고 초기 재아키텍처 계획도 provider stub까지 동등성 범위에 포함했다. 그러나 현재 프로젝트의 목표와 포트폴리오 증거에는 social provider 연동이 필수적이지 않으며, 이를 미리 구현하면 provider 계약, redirect·callback, 계정 충돌·연결·해제와 secret 운영 범위가 추가된다. 사용자는 Kakao·Naver 회원가입·로그인을 제공하지 않고 실제 필요가 생길 때 별도로 도입하기로 결정했다.

### Decision

- 현재 회원 인증은 이메일·비밀번호 Credentials, Spring Security와 PostgreSQL 서버 세션으로 한정한다.
- 이메일 인증, 비밀번호 변경·재설정, session 폐기, CSRF, rate limit과 감사 로그를 현재 인증 완성도 범위로 삼는다.
- Kakao·Naver OAuth endpoint, provider adapter·stub, account link·unlink schema와 테스트를 만들지 않는다.
- Legacy의 Kakao 공유 기능은 인증과 별개인 콘텐츠 공유 entry이므로 해당 화면 parity 범위에 남긴다.
- Social login 필요가 실제로 확인되면 provider 선택, identity 충돌·병합 정책, redirect 보안, secret과 장애 처리까지 새 요구사항과 ADR로 결정한다.
- 이 결정은 ADR-0009, ADR-0029와 ADR-0030의 OAuth 구현·stub 관련 조항만 대체하며 해당 ADR의 서버 세션과 showcase 운영 결정은 유지한다.

### Alternatives

- Kakao·Naver를 지금 실제 연동: legacy parity는 높지만 현재 목표에 필요하지 않은 외부 연동과 운영 책임이 생긴다.
- Provider stub만 구현: 외부 secret 없이 흐름을 보여줄 수 있지만 제공하지 않을 기능의 schema와 정책을 제품에 선반영한다.

### Consequences

- Identity module과 browser E2E는 Credentials 수명주기와 authorization에 집중한다.
- Social account collision·link·unlink는 현재 완료 조건과 포트폴리오 증거에서 제외된다.
- 향후 social login 도입은 단순 설정 변경이 아니라 별도 product·security 설계와 migration이 필요한 기능 추가가 된다.

### Evidence

- 사용자 결정(2026-08-10): 이 프로젝트는 Kakao·Naver 회원가입·로그인을 제공하지 않고 필요하면 나중에 적용한다.
- `docs/PRD.md`: Credentials-only 사용자 여정과 OAuth 범위 제외를 제품 계약으로 명시한다.
- `docs/TRD.md`: Identity module과 인증 계약에서 OAuth provider·link 저장소를 제외한다.
- `PLAN.md`: OAuth stub 대신 Credentials browser auth parity를 다음 실행 slice로 둔다.
