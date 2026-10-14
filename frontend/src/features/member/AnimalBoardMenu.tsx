import NavigationMenu from "../../components/NavigationMenu";
import { ANIMAL_BOARD_OPTIONS } from "./AnimalBoardCatalog";

const links: ReadonlyArray<readonly [string, string]> = [
  ["전체 동물 게시판", "/animals/all"],
  ...ANIMAL_BOARD_OPTIONS.map(({ code, label }) => [`${label} 게시판`, `/animals/${code.toLowerCase()}`] as const),
];

export default function AnimalBoardMenu() {
  return <NavigationMenu label="동물 게시판" links={links} className="animal-board-menu" />;
}
