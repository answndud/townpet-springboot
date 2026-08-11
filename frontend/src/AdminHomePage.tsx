import { Link } from "react-router-dom";

export default function AdminHomePage() {
  return <main className="page placeholder-page"><section className="surface-card profile-card"><p className="eyebrow">TOWNPET OPERATIONS</p><h1>운영 콘솔</h1><p>moderator 권한으로 신고와 미디어 lifecycle을 확인합니다.</p><div className="profile-actions"><Link className="button button-primary" to="/admin/reports">신고 큐</Link><Link className="button button-soft" to="/admin/ops">운영 작업</Link></div></section></main>;
}
