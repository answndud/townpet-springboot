# PLAN.md

## Goal

보안 개선의 남은 외부 운영 증거를 실제 값으로 마무리하고, 확인하지 못한 항목은 추측하지 않고 명확한 입력 대기 상태로 남긴다. MFA는 추가 구현하지 않는다.

## Active

### P3 - 외부 운영 증거 마무리

1. 실제 계정 메일 전달과 DNS 인증을 검증한다.
   - 현재: Resend domain/DKIM `verified`, `send.mail.townpet.cloud` SPF 확인, SMTP STARTTLS PASS. DMARC TXT와 실제 수신함은 미완료다.
   - 다음: 사용자가 DMARC DNS 값과 테스트 수신함을 제공하면 verification/password reset 전달·링크 host/scheme·Outbox 상태를 기록한다.
   - 완료: 실제 수신함에서 두 메일의 전달과 링크 검증이 재현된다.

2. paired backup 복구와 실패 알림을 운영화한다.
   - 현재: non-empty fixture `1→1` restore PASS, backup `2초`, restore `2초`, RPO age 약 18분, 임시 localhost failure webhook script test PASS. `28일 rolling + 최소 7개` retention script와 VPS 매일 `03:45` cron은 배포·dry-run PASS다. production webhook과 실제 삭제 run은 미완료다.
   - 다음: 승인된 failure webhook URL을 구성하고 production failure 수신 및 실제 retention run을 검증한다.
   - 완료: production failure-domain 알림·retention 실행·복구 목표가 실행 evidence로 남는다.

3. MinIO owner isolation을 브라우저에서 검증한다.
   - 현재: anonymous root `403`, preflight `204`, 단일 origin PASS. 최신 배포 후 browser synthetic upload PASS. owner read `200`, 다른 member `404`, guest `401` API 경계 PASS. 게시글 상세에 media viewer가 없고 member delete endpoint도 없다.
   - 다음: media viewer가 제품 범위에 추가될 때 signed object의 실제 browser read를 검증한다. 현재는 소유권 API 경계를 완료 evidence로 유지한다.
   - 완료: 현재 제품이 제공하는 upload와 read authorization 경계가 실제 요청에서 유지된다.

4. VPS workload alert를 운영화한다.
   - 현재: DB `19/100`, backend memory `31.37%`, storage `7%`, threshold `80/90%`, 수동 checker PASS. VPS 매시 `17분` cron은 등록됐고 webhook destination만 미구성이다.
   - 다음: 승인된 alert webhook URL을 구성하고 scheduler에서 WARN/CRITICAL 수신을 검증한다.
   - 완료: workload 초과를 자동 감지·전달하는 운영 evidence가 남는다.
