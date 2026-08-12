import { FormEvent, useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, catalogApi, memberApi } from "./api/client";
import type { Member, Neighborhood } from "./api/client";
import { useAuth } from "./auth/AuthContext";
import { useAbortableRequest } from "./hooks/useAbortableRequest";

type PetDraft = {
  key: string;
  name: string;
  species: string;
};

export default function OnboardingPage() {
  const navigate = useNavigate();
  const { member: authMember, status: authStatus } = useAuth();
  const { data: neighborhoods, error: neighborhoodError, loading: neighborhoodsLoading } = useAbortableRequest<Neighborhood[]>((signal) => catalogApi.neighborhoods(signal), []);
  const [updatedMember, setUpdatedMember] = useState<Member | null>(null);
  const [bio, setBio] = useState("");
  const [neighborhoodId, setNeighborhoodId] = useState("");
  const [pets, setPets] = useState<PetDraft[]>([]);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const nextPetKey = useRef(0);
  const createPetDraft = (name = "", species = "DOG"): PetDraft => {
    nextPetKey.current += 1;
    return { key: `pet-${nextPetKey.current}`, name, species };
  };
  const member = updatedMember ?? authMember;
  const options = neighborhoods ?? [];
  const loading = authStatus === "loading" || neighborhoodsLoading;
  useEffect(() => {
    if (authStatus === "anonymous") { navigate("/login?next=/onboarding", { replace: true }); return; }
    if (authMember?.role === "MODERATOR") { navigate("/admin", { replace: true }); return; }
    if (!authMember || !neighborhoods) return;
    setBio(authMember.bio ?? "");
    setNeighborhoodId(authMember.neighborhoodId ?? neighborhoods[0]?.id ?? "");
    setPets(authMember.pets.map((pet) => createPetDraft(pet.name, pet.species)));
  }, [authMember, authStatus, navigate, neighborhoods]);
  useEffect(() => { if (authStatus === "error" || neighborhoodError) setError("온보딩 정보를 불러오지 못했습니다."); }, [authStatus, neighborhoodError]);

  function updatePet(key: string, field: "name" | "species", value: string) {
    setPets((current) =>
      current.map((pet) => (pet.key === key ? { ...pet, [field]: value } : pet)),
    );
    setSaved(false);
  }

  function removePet(key: string) {
    setPets((current) => current.filter((pet) => pet.key !== key));
    setSaved(false);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      const updated = await memberApi.updateOnboarding({
        bio: bio.trim(),
        neighborhoodId,
        pets: pets.map((pet) => ({ name: pet.name.trim(), species: pet.species })),
      });
      setUpdatedMember(updated);
      setSaved(true);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 400
          ? "동네와 반려동물 입력값을 확인해 주세요."
          : "온보딩 정보를 저장하지 못했습니다.",
      );
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <main className="page onboarding-page">
        <section className="surface-card" role="status">
          온보딩 정보를 불러오는 중...
        </section>
      </main>
    );
  }

  if (error && !member) {
    return (
      <main className="page onboarding-page">
        <section className="surface-card">
          <p role="alert" className="form-error">
            {error}
          </p>
          <Link className="button button-primary" to="/login?next=/onboarding">
            로그인으로 이동
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="page onboarding-page">
      <header className="onboarding-heading">
        <span className="eyebrow">MY TOWNPET</span>
        <h1>내 동네와 반려동물 설정</h1>
        <p>{member?.nickname}님에게 맞는 동네 정보와 반려동물 프로필을 준비합니다.</p>
      </header>

      <form className="onboarding-form" onSubmit={save}>
        <section className="surface-card form-section">
          <div>
            <h2>내 동네</h2>
            <p>피드와 지역 정보를 확인할 대표 동네를 선택해 주세요.</p>
          </div>
          <label>
            대표 동네
            <select
              data-testid="onboarding-neighborhood"
              required
              value={neighborhoodId}
              onChange={(event) => {
                setNeighborhoodId(event.target.value);
                setSaved(false);
              }}
            >
              <option value="" disabled>
                동네를 선택해 주세요
              </option>
              {options.map((neighborhood) => (
                <option key={neighborhood.id} value={neighborhood.id}>
                  {neighborhood.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            소개 (선택)
            <textarea
              maxLength={500}
              value={bio}
              onChange={(event) => {
                setBio(event.target.value);
                setSaved(false);
              }}
              placeholder="나와 반려동물을 간단히 소개해 주세요."
            />
            <span className="field-help">{bio.length}/500</span>
          </label>
        </section>

        <section className="surface-card form-section">
          <div className="section-heading-row">
            <div>
              <h2>반려동물</h2>
              <p>최대 10마리까지 등록할 수 있습니다.</p>
            </div>
            <button
              type="button"
              className="button button-soft"
              disabled={pets.length >= 10}
              onClick={() => setPets((current) => [...current, createPetDraft()])}
            >
              반려동물 추가
            </button>
          </div>
          <div className="pet-drafts">
            {pets.map((pet, index) => (
              <fieldset key={pet.key} className="pet-draft">
                <legend>반려동물 {index + 1}</legend>
                <label>
                  이름
                  <input
                    required
                    maxLength={40}
                    value={pet.name}
                    onChange={(event) => updatePet(pet.key, "name", event.target.value)}
                  />
                </label>
                <label>
                  종류
                  <select
                    value={pet.species}
                    onChange={(event) => updatePet(pet.key, "species", event.target.value)}
                  >
                    <option value="DOG">강아지</option>
                    <option value="CAT">고양이</option>
                    <option value="OTHER">기타</option>
                  </select>
                </label>
                <button
                  type="button"
                  className="text-button"
                  onClick={() => removePet(pet.key)}
                  aria-label={`반려동물 ${index + 1} 삭제`}
                >
                  삭제
                </button>
              </fieldset>
            ))}
          </div>
        </section>

        {error ? (
          <p role="alert" className="form-error" aria-live="polite">
            {error}
          </p>
        ) : null}
        {saved ? (
          <p className="form-success" role="status" aria-live="polite">
            내 동네와 반려동물 정보가 저장되었습니다. <Link to="/profile">프로필 보기</Link>
          </p>
        ) : null}
        <button
          data-testid="onboarding-submit"
          className="button button-primary onboarding-submit"
          type="submit"
          disabled={saving || !neighborhoodId || pets.some((pet) => !pet.name.trim())}
        >
          {saving ? "저장 중..." : "설정 저장"}
        </button>
      </form>
    </main>
  );
}
