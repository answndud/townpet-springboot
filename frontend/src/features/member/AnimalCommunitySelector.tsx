import { ANIMAL_BOARD_GROUPS } from "./AnimalBoardCatalog";

const ANIMAL_COMMUNITY_CODES = new Set(
  ANIMAL_BOARD_GROUPS.flatMap((group) => group.options.map((option) => option.code)),
);

export function initialAnimalCommunityCodes(searchParams: URLSearchParams, fallback?: string) {
  const code = (searchParams.get("animal") ?? fallback ?? "").trim().toUpperCase();
  return ANIMAL_COMMUNITY_CODES.has(code) ? [code] : [];
}

type AnimalCommunitySelectorProps = {
  value: string[];
  onChange: (codes: string[]) => void;
  label?: string;
  help?: string;
};

export default function AnimalCommunitySelector({
  value,
  onChange,
  label = "동물 게시판 분류",
  help = "여러 동물을 선택하면 각 동물 게시판에 함께 노출됩니다.",
}: AnimalCommunitySelectorProps) {
  const selected = new Set(value);

  function toggle(code: string) {
    onChange(selected.has(code) ? value.filter((item) => item !== code) : [...value, code]);
  }

  return (
    <fieldset className="animal-community-selector">
      <legend>{label}</legend>
      <p className="field-help">{help}</p>
      <div className="animal-board-groups">
        {ANIMAL_BOARD_GROUPS.map((group) => (
          <div className="animal-board-group" key={group.label}>
            <strong>{group.label}</strong>
            <div className="animal-board-options">
              {group.options.map((option) => (
                <label key={option.code}>
                  <input
                    type="checkbox"
                    checked={selected.has(option.code)}
                    onChange={() => toggle(option.code)}
                  />
                  {option.label}
                </label>
              ))}
            </div>
          </div>
        ))}
      </div>
    </fieldset>
  );
}
