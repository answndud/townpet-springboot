export type AnimalBoardOption = { code: string; label: string };
export type AnimalBoardGroup = { label: string; options: AnimalBoardOption[] };

export const ANIMAL_BOARD_GROUPS: AnimalBoardGroup[] = [
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

export const ANIMAL_BOARD_OPTIONS = ANIMAL_BOARD_GROUPS.flatMap((group) => group.options);
