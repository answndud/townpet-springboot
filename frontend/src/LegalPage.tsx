import { Link, useLocation } from "react-router-dom";

export default function LegalPage() {
  const { pathname } = useLocation();
  const privacy = pathname === "/privacy";
  return <main className="page placeholder-page"><article className="surface-card legal-card"><p className="eyebrow">TOWNPET POLICY</p><h1>{privacy ? "개인정보 처리방침" : "이용약관"}</h1><p>{privacy ? "TownPet portfolio sandbox는 합성 demo 계정만 사용하며 실제 개인정보를 수집하지 않습니다." : "TownPet은 반려생활 정보를 공유하는 portfolio sandbox입니다. 공개 콘텐츠와 신고 기능은 운영 정책에 따라 관리됩니다."}</p><h2>{privacy ? "수집하지 않는 정보" : "서비스 이용"}</h2><p>{privacy ? "실제 주민등록번호, 연락처, 정확한 위치와 같은 민감한 개인정보를 저장하거나 공개하지 않습니다." : "다른 회원의 권리를 침해하거나 허위·불법 정보를 게시해서는 안 됩니다."}</p><Link className="button button-soft" to="/">홈으로</Link></article></main>;
}
