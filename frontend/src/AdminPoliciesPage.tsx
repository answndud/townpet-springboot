import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminPolicyApi, ApiError, type PolicyDocument } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";

export default function AdminPoliciesPage() {
  const [key, setKey] = useState("TERMS");
  const { data: item, error: requestError, loading, retry } = useAbortableRequest<PolicyDocument>((signal) => adminPolicyApi.get(key, signal), [key]);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadError = requestError ? "정책을 불러오지 못했습니다." : null;
  const error = loadError ?? actionError;

  useEffect(() => {
    if (item) {
      setTitle(item.title);
      setBody(item.body);
    }
  }, [item]);

  async function save() {
    if (pending) return;
    setPending(true); setActionError(null); setNotice(null);
    try { await adminPolicyApi.update({ key, title: title.trim(), body: body.trim() }); setNotice("정책을 저장했습니다."); retry(); }
    catch (requestError) { setActionError(requestError instanceof ApiError ? "정책을 저장하지 못했습니다." : "오류가 발생했습니다."); }
    finally { setPending(false); }
  }

  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">POLICY ADMIN</p><h1>정책 관리</h1><p>공개 이용약관과 개인정보 처리방침을 관리합니다.</p></section><div className="localcare-tabs"><button className={key === "TERMS" ? "market-filter active" : "market-filter"} type="button" onClick={() => { setKey("TERMS"); setTitle(""); setBody(""); setNotice(null); }}>이용약관</button><button className={key === "PRIVACY" ? "market-filter active" : "market-filter"} type="button" onClick={() => { setKey("PRIVACY"); setTitle(""); setBody(""); setNotice(null); }}>개인정보 처리방침</button></div>{error ? <p role="alert">{error}</p> : null}{notice ? <p role="status">{notice}</p> : null}{loading ? <section className="surface-card" role="status">정책을 불러오는 중...</section> : <section className="surface-card publication-fields"><label>제목<input value={title} onChange={(e) => setTitle(e.target.value)} /></label><label>본문<textarea value={body} onChange={(e) => setBody(e.target.value)} /></label><button className="button button-primary" disabled={pending} type="button" onClick={() => void save()}>{pending ? "저장 중..." : "정책 저장"}</button>{item ? <small>최종 수정: {new Date(item.updatedAt).toLocaleString("ko-KR")}</small> : null}</section>}<Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
