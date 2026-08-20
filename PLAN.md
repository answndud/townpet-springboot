# PLAN.md

## Goal

공개 쓰기 abuse와 취약점 스캔 공백을 먼저 닫고, 업로드·rate limit·moderator MFA·계정 전달·복구의 남은 Medium 보안 경계를 실제 운영 증거까지 완성한다. 비회원 콘텐츠 생성은 일일 quota가 아니라 IP·guest별 **시간당** 제한을 사용한다.

## Active

### P3 - 외부 운영 증거 마무리

1. 실제 계정 메일 전달과 DNS 인증을 검증한다.
   - 현재: Resend domain `mail.townpet.cloud` verified, DKIM 확인, SMTP STARTTLS PASS. SPF·DMARC와 실제 수신함은 미검증이다.
   - 다음: DNS에 provider 요구 SPF·DMARC를 등록하고, 별도 테스트 수신함으로 verification/password reset을 보내 링크 host/scheme와 delivery 상태를 기록한다.
   - 완료: 운영 recovery·verification 메일의 실제 전달과 실패 관측이 재현된다.

2. paired backup 복구와 실패 알림을 운영화한다.
   - 현재: backup `20260820T024434Z` 생성·암호화·offsite/local checksum·decrypt·disposable DB/MinIO restore PASS. media object는 0개다.
   - 다음: 실제 failure webhook URL을 구성하고 실패 수신을 재현하며, retention policy·RPO/RTO를 측정하고 non-empty media fixture restore를 추가한다.
   - 완료: 최신 backup의 외부 failure-domain 보관, 복구 가능성, 실패 감지가 실행 evidence로 남는다.

3. MinIO owner isolation을 브라우저에서 검증한다.
   - 현재: anonymous media root `403`, preflight `204`, 단일 허용 origin PASS. 로그인 owner와 다른 member/guest의 signed URL 경계는 미검증이다.
   - 다음: 안전한 테스트 계정과 fixture로 owner upload/read/delete, 타 사용자·guest 접근 거부를 브라우저에서 확인한다.
   - 완료: 실제 브라우저 요청에서도 object ownership과 private bucket 경계가 유지된다.

## Backlog

- VPS workload 측정 후 DB connection·JVM memory·storage 경보 임계값 확정
