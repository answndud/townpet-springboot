import { FormEvent, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { adoptionApi, ApiError } from "./api/client";
import AnimalCommunitySelector, { initialAnimalCommunityCodes } from "./features/member/AnimalCommunitySelector";

export default function AdoptionCreatePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialCodes = initialAnimalCommunityCodes(searchParams, "DOG");
  const [animalCommunityCodes, setAnimalCommunityCodes] = useState<string[]>(initialCodes);
  const initialAnimal = initialCodes[0] ?? "DOG";
  const [form, setForm] = useState({ title: "", description: "", species: initialAnimal, breed: "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    setError(null);
    try {
      const item = await adoptionApi.create({
        ...form,
        breed: form.breed.trim() || undefined,
        animalCommunityCodes,
      });
      navigate(`/adoptions/${item.id}`);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(`/login?next=${encodeURIComponent("/adoptions/new")}`);
        return;
      }
      setError("입양 정보를 확인해 주세요.");
    } finally {
      setSaving(false);
    }
  }

  function changeAnimals(codes: string[]) {
    setAnimalCommunityCodes(codes);
    setForm((current) => ({ ...current, species: codes[0] ?? "OTHER" }));
  }

  return (
    <main className="page marketplace-page">
      <section className="marketplace-hero"><div><p className="eyebrow">ADOPTION</p><h1>입양 글 작성</h1><p>입양 커뮤니티와 동물 분류를 함께 선택해 주세요.</p></div></section>
      <form className="surface-card publication-fields" onSubmit={submit}>
        <AnimalCommunitySelector value={animalCommunityCodes} onChange={changeAnimals} />
        <label>제목<input required maxLength={120} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
        <label>설명<textarea required maxLength={5000} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
        <label>품종<input maxLength={80} value={form.breed} onChange={(event) => setForm({ ...form, breed: event.target.value })} /></label>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <div className="publication-submit-row"><Link className="publication-text-link" to="/animals/all/adoption">취소</Link><button className="button button-primary" disabled={saving} type="submit">{saving ? "등록 중..." : "입양 글 등록"}</button></div>
      </form>
    </main>
  );
}
