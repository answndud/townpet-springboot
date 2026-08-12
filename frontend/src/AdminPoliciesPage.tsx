import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminPolicyApi, ApiError, type PolicyDocument } from "./api/client";

export default function AdminPoliciesPage() {
  const [key, setKey] = useState("TERMS"); const [item, setItem] = useState<PolicyDocument | null>(null); const [title, setTitle] = useState(""); const [body, setBody] = useState(""); const [error, setError] = useState<string | null>(null);
  useEffect(() => { adminPolicyApi.get(key).then((next) => { setItem(next); setTitle(next.title); setBody(next.body); }).catch(() => setError("정책을 불러오지 못했습니다.")); }, [key]);
  async function save() { try { const next = await adminPolicyApi.update({ key, title, body }); setItem(next); setError("정책을 저장했습니다."); } catch (e) { setError(e instanceof ApiError ? "정책을 저장하지 못했습니다." : "오류가 발생했습니다."); } }
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">POLICY ADMIN</p><h1>정책 관리</h1><p>공개 이용약관과 개인정보 처리방침을 관리합니다.</p></section><div className="localcare-tabs"><button className={key === "TERMS" ? "market-filter active" : "market-filter"} onClick={() => setKey("TERMS")}>이용약관</button><button className={key === "PRIVACY" ? "market-filter active" : "market-filter"} onClick={() => setKey("PRIVACY")}>개인정보 처리방침</button></div>{error ? <p role="alert">{error}</p> : null}<section className="surface-card publication-fields"><label>제목<input value={title} onChange={(e) => setTitle(e.target.value)} /></label><label>본문<textarea value={body} onChange={(e) => setBody(e.target.value)} /></label><button className="button button-primary" type="button" onClick={() => void save()}>정책 저장</button>{item ? <small>최종 수정: {new Date(item.updatedAt).toLocaleString("ko-KR")}</small> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
