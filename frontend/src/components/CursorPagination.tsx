type CursorPaginationProps = {
  page: number;
  hasNext: boolean;
  hasPrevious: boolean;
  onPageChange: (page: number) => void;
  disabled?: boolean;
};

export default function CursorPagination({ page, hasNext, hasPrevious, onPageChange, disabled = false }: CursorPaginationProps) {
  if (page === 1 && !hasNext) return null;
  const firstVisiblePage = page > 3 ? page - 2 : 1;
  const lastVisiblePage = page + Number(hasNext);
  const visiblePages = Array.from(
    { length: Math.min(5, lastVisiblePage - firstVisiblePage + 1) },
    (_, index) => firstVisiblePage + index,
  );
  return (
    <nav className="feed-pagination" aria-label="게시글 페이지 이동">
      <button type="button" className="feed-page-arrow" aria-label="이전 페이지" title="이전 페이지" disabled={disabled || !hasPrevious} onClick={() => onPageChange(page - 1)}>&lt;</button>
      <div className="feed-page-numbers" aria-label="페이지 번호">
        {firstVisiblePage > 1 ? <><button type="button" className="feed-page-number" aria-label="첫 페이지" onClick={() => onPageChange(1)}>1</button><span className="feed-page-ellipsis" aria-hidden="true">…</span></> : null}
        {visiblePages.map((pageNumber) => (
          <button
            key={pageNumber}
            type="button"
            className={`feed-page-number${pageNumber === page ? " active" : ""}`}
            aria-label={`${pageNumber}페이지`}
            aria-current={pageNumber === page ? "page" : undefined}
            disabled={disabled || pageNumber === page}
            onClick={() => onPageChange(pageNumber)}
          >
            {pageNumber}
          </button>
        ))}
      </div>
      <button type="button" className="feed-page-arrow" aria-label="다음 페이지" title="다음 페이지" disabled={disabled || !hasNext} onClick={() => onPageChange(page + 1)}>&gt;</button>
    </nav>
  );
}
