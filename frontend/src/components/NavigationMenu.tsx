import { KeyboardEvent, useEffect, useLayoutEffect, useRef, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";

type Props = {
  label: string;
  links: ReadonlyArray<readonly [string, string]>;
  className?: string;
  onIntent?: (href: string) => void;
};

export default function NavigationMenu({ label, links, className = "", onIntent }: Props) {
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const menu = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const items = useRef<Array<HTMLAnchorElement | null>>([]);
  const pointer = useRef("");
  const pendingFocus = useRef<number | null>(null);
  const active = links.some(([, href]) => location.pathname === href || location.pathname.startsWith(`${href}/`));
  const menuId = `navigation-${label.replace(/\s+/g, "-")}`;

  useEffect(() => { setOpen(false); }, [location.pathname, location.search]);
  useEffect(() => {
    if (!open) return;
    function outside(event: PointerEvent) {
      if (!menu.current?.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener("pointerdown", outside);
    return () => document.removeEventListener("pointerdown", outside);
  }, [open]);

  useLayoutEffect(() => {
    if (open && pendingFocus.current !== null) {
      focus(pendingFocus.current);
      pendingFocus.current = null;
    }
  }, [open]);

  function focus(index: number) {
    items.current[(index + links.length) % links.length]?.focus();
  }
  function keyboard(event: KeyboardEvent<HTMLDivElement>) {
    const index = items.current.indexOf(document.activeElement as HTMLAnchorElement);
    if (event.key === "Escape") {
      event.preventDefault(); setOpen(false); trigger.current?.focus();
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      const next = index < 0 ? (event.key === "ArrowDown" ? 0 : links.length - 1) : index + (event.key === "ArrowDown" ? 1 : -1);
      if (open) focus(next);
      else { pendingFocus.current = next; setOpen(true); }
    } else if (open && (event.key === "Home" || event.key === "End")) {
      event.preventDefault(); focus(event.key === "Home" ? 0 : links.length - 1);
    }
  }

  return <div ref={menu} className={`header-menu ${className}${open ? " open" : ""}`}
    onPointerEnter={event => { if (event.pointerType === "mouse") setOpen(true); }}
    onPointerLeave={event => { if (event.pointerType === "mouse") setOpen(false); }}
    onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setOpen(false); }}
    onKeyDown={keyboard}>
    <button ref={trigger} className={`header-menu-trigger${active ? " active" : ""}`} type="button"
      aria-haspopup="menu" aria-expanded={open} aria-controls={menuId}
      onPointerDown={event => { pointer.current = event.pointerType; }}
      onClick={event => { setOpen(current => event.detail > 0 && pointer.current === "mouse" ? true : !current); pointer.current = ""; }}>
      {label}<span aria-hidden="true">⌄</span>
    </button>
    <div id={menuId} className="header-menu-panel" role="menu" aria-label={`${label} 바로가기`}>
      {links.map(([title, href], index) => <NavLink key={href} ref={element => { items.current[index] = element; }} role="menuitem" to={href}
        onMouseEnter={() => onIntent?.(href)} onFocus={() => onIntent?.(href)} onClick={() => setOpen(false)}>{title}</NavLink>)}
    </div>
  </div>;
}
