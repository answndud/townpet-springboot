import { KeyboardEvent as ReactKeyboardEvent, useEffect, useRef, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { ANIMAL_BOARD_OPTIONS } from "./AnimalBoardCatalog";

const MENU_LINKS = [
  ["전체 동물 게시판", "/animals/all"],
  ...ANIMAL_BOARD_OPTIONS.map(({ code, label }) => [`${label} 게시판`, `/animals/${code.toLowerCase()}`]),
] as const;

export default function AnimalBoardMenu() {
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const menuItemsRef = useRef<HTMLAnchorElement[]>([]);

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
    <div ref={menuRef} className={`header-menu animal-board-menu${open ? " open" : ""}`}>
      <button
        className="header-menu-trigger"
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls="animal-board-menu"
        onClick={() => setOpen((current) => !current)}
        onKeyDown={handleKeyDown}
      >
        동물 게시판<span aria-hidden="true">⌄</span>
      </button>
      <div id="animal-board-menu" className="header-menu-panel animal-board-menu-panel" role="menu" aria-label="동물 게시판" onKeyDown={handleMenuKeyDown}>
        <div className="animal-board-menu-heading">
          <strong>동물 게시판</strong>
          <span>동물별 게시판으로 이동</span>
        </div>
        {MENU_LINKS.map(([label, href], index) => (
          <NavLink
            key={href}
            ref={(element) => { if (element) menuItemsRef.current[index] = element; }}
            role="menuitem"
            to={href}
            onClick={() => setOpen(false)}
          >
            {label}
          </NavLink>
        ))}
      </div>
    </div>
  );
}
