import { KeyboardEvent as ReactKeyboardEvent, useEffect, useMemo, useRef, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { memberApi } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";

export type AnimalInterestOption = { code: string; label: string };
export type AnimalInterestGroup = { label: string; options: AnimalInterestOption[] };

export const ANIMAL_INTEREST_GROUPS: AnimalInterestGroup[] = [
  { label: "강아지 & 고양이", options: [{ code: "DOG", label: "강아지" }, { code: "CAT", label: "고양이" }] },
  { label: "조류", options: [{ code: "PARROT", label: "앵무새" }, { code: "BIRD", label: "조류" }] },
  {
    label: "파충류 & 양서류",
    options: [
      { code: "TURTLE", label: "거북" },
      { code: "LIZARD", label: "도마뱀" },
      { code: "SNAKE", label: "뱀" },
      { code: "AMPHIBIAN", label: "양서류" },
      { code: "REPTILE", label: "파충류" },
    ],
  },
  { label: "소동물", options: [{ code: "SMALL_ANIMAL", label: "소동물" }] },
  { label: "어류 / 수조", options: [{ code: "AQUARIUM_FISH", label: "어류·수조" }] },
  { label: "기타", options: [{ code: "ARTHROPOD_INSECT", label: "절지류·곤충" }] },
];

const ALL_CODES = ANIMAL_INTEREST_GROUPS.flatMap((group) => group.options.map((option) => option.code));
const STORAGE_PREFIX = "townpet:animal-interests:v1";

function storageKey(memberId?: string) {
  return memberId ? `${STORAGE_PREFIX}:member:${memberId}` : `${STORAGE_PREFIX}:guest`;
}

export function readStoredAnimalInterests(memberId?: string): string[] | null {
  try {
    const raw =
      window.localStorage.getItem(storageKey(memberId))
      ?? (!memberId ? window.localStorage.getItem(STORAGE_PREFIX) : null);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return null;
    const selected = parsed.filter((code): code is string => typeof code === "string" && ALL_CODES.includes(code));
    return [...new Set(selected)];
  } catch {
    return null;
  }
}

function readStoredInterests(memberId?: string) {
  return readStoredAnimalInterests(memberId) ?? ALL_CODES;
}

function storeInterests(codes: string[], memberId?: string) {
  try {
    window.localStorage.setItem(storageKey(memberId), JSON.stringify(codes));
  } catch {
    // Private browsing and restricted storage should not block the menu.
  }
}

export function AnimalInterestSettings({ embedded = false }: { embedded?: boolean } = {}) {
  const { member } = useAuth();
  const isMember = member?.role === "MEMBER";
  const memberId = isMember ? member.id : undefined;
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<string[]>(readStoredInterests);
  const [saving, setSaving] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const menuRef = useRef<HTMLDivElement>(null);
  const panelOpen = open || embedded;

  useEffect(() => {
    if (!panelOpen) return;
    if (!isMember) {
      setSelected(readStoredInterests());
      return;
    }
    let active = true;
    memberApi
      .animalInterests()
      .then((codes) => {
        if (!active || !Array.isArray(codes)) return;
        setSelected(codes.filter((code) => ALL_CODES.includes(code)));
      })
      .catch(() => {
        setSelected(readStoredInterests(memberId));
      });
    return () => {
      active = false;
    };
  }, [isMember, memberId, panelOpen]);

  useEffect(() => {
    if (!open || embedded) return;
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (event.target instanceof Node && !menuRef.current?.contains(event.target)) setOpen(false);
    };
    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, [embedded, open]);

  const selectedSet = useMemo(() => new Set(selected), [selected]);
  const allSelected = selected.length === ALL_CODES.length;

  function toggle(code: string) {
    setSelected((current) => (current.includes(code) ? current.filter((item) => item !== code) : [...current, code]));
    setStatusMessage("");
  }

  function selectAll() {
    setSelected(ALL_CODES);
    setStatusMessage("");
  }

  function clearAll() {
    setSelected([]);
    setStatusMessage("");
  }

  async function save() {
    setSaving(true);
    try {
      if (isMember) await memberApi.updateAnimalInterests(selected);
      storeInterests(selected, memberId);
      window.dispatchEvent(
        new CustomEvent("townpet:animal-interests-change", {
          detail: { codes: selected, memberId },
        }),
      );
      setStatusMessage("관심 동물을 저장했습니다.");
    } catch {
      storeInterests(selected, memberId);
      setStatusMessage("서버와 연결되지 않아 이 브라우저에 임시 저장했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div ref={menuRef} className={`header-menu interest-menu${panelOpen ? " open" : ""}`}>
      {!embedded ? <button
        className="header-menu-trigger"
        type="button"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls="animal-interest-menu"
        onClick={() => setOpen((current) => !current)}
      >
        관심 동물<span aria-hidden="true">⌄</span>
      </button> : null}
      <div
        id="animal-interest-menu"
        className="header-menu-panel interest-menu-panel"
        role={embedded ? "region" : "dialog"}
        aria-label="관심 동물 설정"
      >
        <p className="interest-menu-description">보고 싶은 동물을 체크하고 저장하세요.</p>
        <div className="interest-groups">
          {ANIMAL_INTEREST_GROUPS.map((group) => (
            <fieldset className="interest-group" key={group.label}>
              <legend>{group.label}</legend>
              <div className="interest-options">
                {group.options.map((option) => (
                  <label key={option.code}>
                    <input
                      type="checkbox"
                      checked={selectedSet.has(option.code)}
                      onChange={() => toggle(option.code)}
                    />
                    {option.label}
                  </label>
                ))}
              </div>
            </fieldset>
          ))}
        </div>
        <div className="interest-actions">
          <button className="button button-soft" type="button" onClick={selectAll} disabled={allSelected}>전체 선택</button>
          <button className="button button-soft" type="button" onClick={clearAll} disabled={!selected.length}>전체 해제</button>
          <button className="button button-primary" type="button" onClick={() => void save()} disabled={saving}>
            {saving ? "저장 중…" : "저장"}
          </button>
        </div>
        {statusMessage ? <p className="interest-status" role="status">{statusMessage}</p> : null}
      </div>
    </div>
  );
}

function animalLabel(code: string) {
  return ANIMAL_INTEREST_GROUPS
    .flatMap((group) => group.options)
    .find((option) => option.code === code)?.label ?? code;
}

export default function AnimalInterestMenu() {
  const { member } = useAuth();
  const location = useLocation();
  const memberId = member?.role === "MEMBER" ? member.id : undefined;
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<string[]>(() => readStoredInterests(memberId));
  const menuRef = useRef<HTMLDivElement>(null);
  const menuItemsRef = useRef<HTMLAnchorElement[]>([]);

  useEffect(() => {
    if (!open) return;
    if (!memberId) {
      setSelected(readStoredInterests());
      return;
    }
    let active = true;
    memberApi.animalInterests()
      .then((codes) => { if (active) setSelected(codes.filter((code) => ALL_CODES.includes(code))); })
      .catch(() => { if (active) setSelected(readStoredInterests(memberId)); });
    return () => { active = false; };
  }, [memberId, open]);

  useEffect(() => {
    if (!open) return;
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (event.target instanceof Node && !menuRef.current?.contains(event.target)) setOpen(false);
    };
    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, [open]);

  useEffect(() => {
    setOpen(false);
  }, [location.pathname, location.search]);

  useEffect(() => {
    const updateSelected = (event: Event) => {
      const detail = (event as CustomEvent<{ codes?: unknown; memberId?: string }>).detail;
      if (detail?.memberId !== memberId || !Array.isArray(detail.codes)) return;
      setSelected(detail.codes.filter((code): code is string => typeof code === "string" && ALL_CODES.includes(code)));
    };
    window.addEventListener("townpet:animal-interests-change", updateSelected);
    return () => window.removeEventListener("townpet:animal-interests-change", updateSelected);
  }, [memberId]);

  const codes = selected;
  useEffect(() => {
    menuItemsRef.current.length = codes.length + 2;
  }, [codes.length]);

  function focusMenuItem(index: number) {
    const items = menuItemsRef.current;
    if (!items.length) return;
    items[(index + items.length) % items.length]?.focus();
  }

  function handleKeyDown(event: ReactKeyboardEvent<HTMLButtonElement>) {
    if (event.key === "Escape") {
      setOpen(false);
      event.currentTarget.focus();
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      setOpen(true);
      window.setTimeout(() => focusMenuItem(event.key === "ArrowDown" ? 0 : -1), 0);
    }
  }

  function handleMenuKeyDown(event: ReactKeyboardEvent<HTMLDivElement>) {
    const currentIndex = menuItemsRef.current.indexOf(document.activeElement as HTMLAnchorElement);
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      focusMenuItem(currentIndex + (event.key === "ArrowDown" ? 1 : -1));
    } else if (event.key === "Home" || event.key === "End") {
      event.preventDefault();
      focusMenuItem(event.key === "Home" ? 0 : -1);
    } else if (event.key === "Escape") {
      event.preventDefault();
      setOpen(false);
      menuRef.current?.querySelector<HTMLButtonElement>(".header-menu-trigger")?.focus();
    }
  }

  return (
    <div ref={menuRef} className={`header-menu interest-menu${open ? " open" : ""}`}>
      <button
        className="header-menu-trigger"
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls="animal-community-menu"
        onClick={() => setOpen((current) => !current)}
        onKeyDown={handleKeyDown}
      >
        관심 동물<span aria-hidden="true">⌄</span>
      </button>
      <div id="animal-community-menu" className="header-menu-panel interest-menu-panel animal-community-menu" role="menu" aria-label="동물 커뮤니티" onKeyDown={handleMenuKeyDown}>
        <div className="animal-community-menu-heading">
          <strong>동물 커뮤니티</strong>
          <span>동물별 게시판으로 이동</span>
        </div>
        <NavLink ref={(element) => { if (element) menuItemsRef.current[0] = element; }} role="menuitem" to="/animals/all" onClick={() => setOpen(false)}>전체 동물</NavLink>
        {codes.map((code, index) => (
          <NavLink key={code} ref={(element) => { if (element) menuItemsRef.current[index + 1] = element; }} role="menuitem" to={`/animals/${code.toLowerCase()}`} onClick={() => setOpen(false)}>
            {animalLabel(code)} 커뮤니티
          </NavLink>
        ))}
        <NavLink ref={(element) => { if (element) menuItemsRef.current[codes.length + 1] = element; }} className="animal-community-settings" role="menuitem" to="/settings/animal-interests" onClick={() => setOpen(false)}>
          관심 동물 관리
        </NavLink>
      </div>
    </div>
  );
}
