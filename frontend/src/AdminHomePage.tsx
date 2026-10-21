import { useState } from "react";
import { Link } from "react-router-dom";
import { adminModerationApi, ApiError } from "./api/client";

export default function AdminHomePage() {
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState<"inspect" | "cleanup" | null>(null);

  async function cleanup(dryRun: boolean) { if (pending) return; setPending(dryRun ? "inspect" : "cleanup"); setError(null); setResult(null); try { const report = await adminModerationApi.mediaCleanup(dryRun); setResult(`${dryRun ? "점검" : "정리"} 완료 · 대상 ${report.candidateCount}건 · ${report.candidateBytes.toLocaleString()} bytes${dryRun ? "" : ` · 삭제 ${report.deletedCount}건`}`); } catch (e) { setError(e instanceof ApiError && [401, 403].includes(e.status) ? "운영자 권한이 필요합니다." : "미디어 정리 작업을 실행하지 못했습니다."); } finally { setPending(null); } }
  return <main className="page admin-page"><section className="admin-console"><h1>운영 콘솔</h1><nav className="admin-navigation" aria-label="운영 기능">
  <section><h2>신고·검토</h2><Link to="/admin/reports">신고 큐</Link><Link to="/admin/comments">댓글 운영 관리</Link><Link to="/admin/corrections">정보 정정</Link><Link to="/admin/hospital-review-flags">병원 후기 검토</Link><Link to="/admin/care-feedbacks">돌봄 피드백</Link><Link to="/admin/moderation/direct">직접 조치</Link></section>
  <section><h2>기록·보안</h2><Link to="/admin/moderation-logs">운영 로그</Link><Link to="/admin/auth-audits">인증 감사</Link><Link to="/admin/mfa">운영자 인증</Link></section>
  <section><h2>서비스 관리</h2><Link to="/admin/breeds">품종 카탈로그</Link><Link to="/admin/personalization">개인화 설정</Link><Link to="/admin/policies">정책 문서</Link></section>
  </nav>{error ? <p role="alert">{error}</p> : null}{result ? <p role="status">{result}</p> : null}<section className="surface-card"><h2>미디어 업로드 정리</h2><div className="profile-actions"><button className="button button-soft" disabled={Boolean(pending)} type="button" onClick={() => void cleanup(true)}>{pending === "inspect" ? "점검 중..." : "만료 업로드 점검"}</button><button className="button button-danger" disabled={Boolean(pending)} type="button" onClick={() => void cleanup(false)}>{pending === "cleanup" ? "삭제 중..." : "만료 업로드 삭제"}</button></div></section></section></main>;
}
