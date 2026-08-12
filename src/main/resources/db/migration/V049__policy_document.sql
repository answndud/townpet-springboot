CREATE TABLE policy_document (
  policy_key VARCHAR(40) PRIMARY KEY,
  title VARCHAR(160) NOT NULL,
  body VARCHAR(20000) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by UUID REFERENCES member_account(id)
);
INSERT INTO policy_document(policy_key, title, body) VALUES
 ('TERMS', '이용약관', 'TownPet은 반려생활 정보를 공유하는 portfolio sandbox입니다.'),
 ('PRIVACY', '개인정보 처리방침', '실제 민감 개인정보를 수집하지 않는 합성 demo 환경입니다.');
