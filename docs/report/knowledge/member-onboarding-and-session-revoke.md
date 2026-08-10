# Member onboarding과 session revoke

## 핵심 개념

온보딩은 회원 profile과 회원 소유 반려동물 목록을 함께 변경하는 command다. controller는 principal과 입력 형식만 확인하고 transaction 및 소유 범위는 Member application 경계에서 보장해야 한다. logout은 브라우저 쿠키를 지우는 UI 동작이 아니라 서버의 인증 session을 폐기하는 보안 command다.

## TownPet 적용

- schema: `member_profile`, `member_pet`
- API: `GET /api/v1/members/me`, `PUT /api/v1/members/me/onboarding`, `DELETE /api/v1/auth/sessions/current`
- code: `MemberController`, `MemberPetRepository`, `SessionController`
- tests: `IdentityMemberControllerTest`

## Failure mode와 대안

반려동물 목록을 controller에서 직접 member id로 받으면 IDOR가 생길 수 있으므로 URL의 member id를 사용하지 않고 principal에서 UUID를 얻는다. 목록이 커지거나 반려동물별 lifecycle이 필요해지면 전체 교체 대신 versioned child command로 전환한다. logout에서 cookie만 만료하면 탈취된 session이 계속 유효할 수 있으므로 session store invalidate를 우선한다.

## 면접 체크

- 왜 onboarding과 pet을 한 요청에 묶었나? 현재 제품 여정과 작은 transaction 경계가 일치하기 때문이다.
- 왜 DB constraint만으로 충분하지 않나? 최대 개수·공백·입력 오류는 API 응답 계약과 함께 일관되게 검증해야 하기 때문이다.
- logout이 성공했다는 증거는? revoke 직후 같은 session으로 보호 API를 호출했을 때 401이 되는 테스트다.
