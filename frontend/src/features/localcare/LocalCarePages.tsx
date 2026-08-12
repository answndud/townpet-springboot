import { FormEvent, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, localResourceApi, type LocalResource, type LocalResourceKind } from "../../api/client";
import { useAbortableRequest } from "../../hooks/useAbortableRequest";
import { formatDateOnly } from "../../utils/date";

const labels: Record<LocalResourceKind, string> = { LOCAL_GUIDE: "지역 가이드", WELFARE: "복지 안내", CARE: "케어 가이드" };

export function LocalCareListPage() {
  const [kind, setKind] = useState<LocalResourceKind | undefined>();
  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const { data: loadedItems, error: requestError, loading } = useAbortableRequest<LocalResource[]>((signal) => localResourceApi.list(kind, submittedQuery, signal), [kind, submittedQuery]);
  const items = loadedItems ?? [];
  const error = requestError ? "정보를 불러오지 못했습니다." : null;

  function load(event: FormEvent) {
    event.preventDefault();
    setSubmittedQuery(query.trim());
  }

  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">LOCAL · WELFARE · CARE</p><h1>우리 동네 반려생활 가이드</h1><p>산책 장소부터 복지 제도와 케어 팁까지, 출처와 갱신 시각을 함께 확인하세요.</p></section><form className="surface-card localcare-search" onSubmit={load}><label>정보 검색<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="산책, 등록, 여름..." /></label><button className="button button-primary" type="submit">검색</button></form><div className="localcare-tabs" role="tablist"><button className={!kind ? "market-filter active" : "market-filter"} type="button" onClick={() => setKind(undefined)}>전체</button>{(Object.keys(labels) as LocalResourceKind[]).map((item) => <button className={kind === item ? "market-filter active" : "market-filter"} key={item} type="button" onClick={() => setKind(item)}>{labels[item]}</button>)}</div>{error ? <p className="form-error" role="alert">{error}</p> : null}<section className="localcare-grid" aria-label="지역 생활 정보" aria-busy={loading}>{items.map((item) => <Link className="surface-card localcare-card" key={item.id} to={`/guides/${item.id}`}><span className="publication-chip publication-chip-primary">{labels[item.kind]}</span><h2>{item.title}</h2><p>{item.summary}</p><small>{item.sourceName} · {formatDateOnly(item.updatedAt)} 업데이트</small></Link>)}</section>{!items.length && !error && !loading ? <p className="surface-card localcare-empty">조건에 맞는 정보가 없습니다.</p> : null}</main>;
}

export function LocalCareDetailPage() {
  const { resourceId = "" } = useParams();
  const { data: item, error: requestError } = useAbortableRequest<LocalResource>((signal) => localResourceApi.detail(resourceId, signal), [resourceId]);
  const error = requestError instanceof ApiError && requestError.status === 404 ? "정보를 찾을 수 없습니다." : requestError ? "정보를 불러오지 못했습니다." : null;
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/guides">목록으로</Link></section></main>;
  if (!item) return <main className="page placeholder-page"><section className="surface-card" role="status">정보를 불러오는 중...</section></main>;
  return <main className="page localcare-page"><Link className="publication-text-link" to="/guides">목록으로</Link><article className="surface-card localcare-detail"><span className="publication-chip publication-chip-primary">{labels[item.kind]}</span><h1>{item.title}</h1><p className="localcare-summary">{item.summary}</p><div className="localcare-content">{item.content}</div><footer><span>출처: {item.sourceName}</span><span>최종 확인: {formatDateOnly(item.updatedAt)}</span>{item.sourceUrl ? <a href={item.sourceUrl} rel="noreferrer" target="_blank">원문 보기</a> : null}</footer></article></main>;
}
