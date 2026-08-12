import { useState } from "react";
import { Link } from "react-router-dom";
import { adminModerationApi, ApiError } from "./api/client";

export default function AdminHomePage() {
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  async function cleanup(dryRun: boolean) { try { const report = await adminModerationApi.mediaCleanup(dryRun); setResult(`${dryRun ? "점검" : "정리"} 완료 · 대상 ${report.expiredCount}건 · ${report.expiredBytes.toLocaleString()} bytes${dryRun ? "" : ` · 삭제 ${report.deletedCount}건`}`); setError(null); } catch (e) { setError(e instanceof ApiError && [401, 403].includes(e.status) ? "운영자 권한이 필요합니다." : "미디어 정리 작업을 실행하지 못했습니다."); } }
  return <main className="page placeholder-page"><section className="surface-card profile-card"><p className="eyebrow">TOWNPET OPERATIONS</p><h1>운영 콘솔</h1><p>moderator 권한으로 신고와 미디어 lifecycle을 확인합니다.</p><div className="profile-actions"><Link className="button button-primary" to="/admin/reports">신고 큐</Link><Link className="button button-soft" to="/admin/moderation-logs">운영 로그</Link></div>{error ? <p role="alert">{error}</p> : null}{result ? <p role="status">{result}</p> : null}<section className="surface-card"><h2>미디어 업로드 정리</h2><p>만료된 미완료 업로드를 먼저 점검한 뒤 삭제합니다.</p><div className="profile-actions"><button className="button button-soft" type="button" onClick={() => void cleanup(true)}>만료 업로드 점검</button><button className="button button-danger" type="button" onClick={() => void cleanup(false)}>만료 업로드 삭제</button></div></section></section></main>;
}
