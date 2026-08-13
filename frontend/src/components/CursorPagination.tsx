type CursorPaginationProps = {
  page: number;
  hasNext: boolean;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
};

export default function CursorPagination({ page, hasNext, totalPages, onPageChange, disabled = false }: CursorPaginationProps) {
  const lastPage = Math.max(1, totalPages);
  const windowStart = Math.max(1, Math.min(page - 2, lastPage - 4));
  const pages = Array.from({ length: Math.min(5, lastPage) }, (_, index) => windowStart + index);
  return (
    <nav className="feed-pagination" aria-label="게시글 페이지 이동">
      <button type="button" className="button button-soft" aria-label="첫 페이지" title="첫 페이지" disabled={disabled || page === 1} onClick={() => onPageChange(1)}>&lt;&lt;</button>
      <button type="button" className="button button-soft" aria-label="이전 페이지" title="이전 페이지" disabled={disabled || page === 1} onClick={() => onPageChange(page - 1)}>&lt;</button>
      <div className="feed-pagination-pages">
        {pages.map((value) => (
          <button key={value} type="button" className={value === page ? "active" : ""} aria-current={value === page ? "page" : undefined} disabled={disabled || value === page} onClick={() => onPageChange(value)}>{value}</button>
        ))}
      </div>
      <button type="button" className="button button-soft" aria-label="다음 페이지" title="다음 페이지" disabled={disabled || page >= lastPage || !hasNext} onClick={() => onPageChange(page + 1)}>&gt;</button>
      <button type="button" className="button button-soft" aria-label="마지막 페이지" title="마지막 페이지" disabled={disabled || page >= lastPage || !hasNext} onClick={() => onPageChange(lastPage)}>&gt;&gt;</button>
    </nav>
  );
}
