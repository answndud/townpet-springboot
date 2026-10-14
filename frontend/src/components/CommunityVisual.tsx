type VisualKind = "paw" | "shop" | "location" | "calendar" | "care" | "guide";
const paths: Record<VisualKind, string> = {
  paw: "M8 13c-4 3-3 8 1 7 2-1 4-1 6 0 4 1 5-4 1-7-2-2-6-2-8 0 M5 7v1 M10 4v2 M15 4v2 M20 7v1",
  shop: "M4 9h16l-2-5H6L4 9Z M5 9v11h14V9 M9 20v-7h6v7 M4 9c0 3 4 3 4 0 0 3 4 3 4 0 0 3 4 3 4 0 0 3 4 3 4 0",
  location: "M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0",
  calendar: "M5 5h14v15H5V5Z M8 3v4 M16 3v4 M5 10h14 M8 14h3 M13 17h3",
  care: "M12 20S3 14 3 8c0-5 6-6 9-2 3-4 9-3 9 2 0 6-9 12-9 12Z",
  guide: "M12 6c-3-2-7-2-9-1v14c3-1 6-1 9 1 3-2 6-2 9-1V5c-3-1-6-1-9 1Z M12 6v14",
};
export function CommunityIcon({ kind = "paw" }: { kind?: VisualKind }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[kind]} /></svg>;
}
export function ContentVisual({ kind, label = "사진 미등록" }: { kind: VisualKind; label?: string }) {
  return <div className={`content-visual visual-${kind}`}><span className="visual-orbit" /><CommunityIcon kind={kind} /><span>{label}</span></div>;
}
export function NeighborhoodIllustration() {
  return <svg className="neighborhood-illustration" viewBox="0 0 440 280" fill="none" aria-hidden="true">
    <ellipse cx="230" cy="240" rx="174" ry="14" fill="#c8dace" />
    <path d="M48 237V133l62-52 63 52v104" fill="#edf2e4" stroke="#9bada4" strokeWidth="3" />
    <path d="m32 138 78-67 79 67" stroke="#405e51" strokeWidth="5" strokeLinecap="round" />
    <path d="M83 237v-62h53v62" fill="#9cbbae" /><rect x="85" y="121" width="50" height="30" rx="5" fill="#f8fbf5" />
    <path d="M346 235V107" stroke="#637c67" strokeWidth="5" /><circle cx="347" cy="101" r="47" fill="#b2c5a4" /><circle cx="373" cy="127" r="31" fill="#c8d4b4" />
    <path d="M200 224c-8-22-3-53 23-61l27-3c22 0 42 17 42 37v30" fill="#d9a879" stroke="#81604b" strokeWidth="3" />
    <path d="m224 169-18-37c-13-21-36-10-31 9l10 36" fill="#b88158" stroke="#81604b" strokeWidth="3" />
    <path d="M214 158c18-18 53-11 59 7l-2 26c-4 19-47 25-57 3Z" fill="#eecaa2" stroke="#81604b" strokeWidth="3" />
    <circle cx="237" cy="179" r="3" fill="#394840" /><path d="m262 182 8 3-7 6" fill="#394840" />
    <path d="M290 185c14-7 14-30 4-32" stroke="#81604b" strokeWidth="7" strokeLinecap="round" />
    <path d="M205 221v16 M276 220v17" stroke="#81604b" strokeWidth="10" strokeLinecap="round" />
    <path d="M138 227c-9-16-9-39 6-50l-1-18 17 9 19-9-2 20c14 15 11 36 3 48" fill="#f8f4e8" stroke="#8a958a" strokeWidth="3" />
    <circle cx="153" cy="187" r="2.5" fill="#405e51" /><circle cx="170" cy="187" r="2.5" fill="#405e51" /><path d="m158 197 4 4 4-4 M140 216c-17-6-21 9-15 15" stroke="#8a958a" strokeWidth="3" strokeLinecap="round" />
    <circle cx="262" cy="57" r="24" fill="#f4d899" /><path d="M205 80c10-10 20-10 30 0" stroke="#9bada4" strokeWidth="3" strokeLinecap="round" />
  </svg>;
}
