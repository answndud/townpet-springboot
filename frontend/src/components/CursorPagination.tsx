type CursorPaginationProps = {
  page: number;
  hasNext: boolean;
  hasPrevious: boolean;
  onPageChange: (page: number) => void;
  disabled?: boolean;
};

export default function CursorPagination({ page, hasNext, hasPrevious, onPageChange, disabled = false }: CursorPaginationProps) {
  if (page === 1 && !hasNext) return null;
  return (
    <nav className="feed-pagination" aria-label="게시글 페이지 이동">
      <button type="button" className="button button-soft" aria-label="이전 페이지" title="이전 페이지" disabled={disabled || !hasPrevious} onClick={() => onPageChange(page - 1)}>&lt;</button>
      <span aria-live="polite">{page}페이지</span>
      <button type="button" className="button button-soft" aria-label="다음 페이지" title="다음 페이지" disabled={disabled || !hasNext} onClick={() => onPageChange(page + 1)}>&gt;</button>
    </nav>
  );
}
