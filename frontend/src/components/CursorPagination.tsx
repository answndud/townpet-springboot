type CursorPaginationProps = {
  page: number;
  hasNext: boolean;
  onPageChange: (page: number) => void;
  disabled?: boolean;
};

export default function CursorPagination({ page, hasNext, onPageChange, disabled = false }: CursorPaginationProps) {
  const pages = Array.from(new Set([page - 1, page, ...(hasNext ? [page + 1] : [])].filter((value) => value > 0)));
  return (
    <nav className="feed-pagination" aria-label="게시글 페이지 이동">
      <button type="button" className="button button-soft" disabled={disabled || page === 1} onClick={() => onPageChange(page - 1)}>이전</button>
      <div className="feed-pagination-pages">
        {pages.map((value) => (
          <button key={value} type="button" className={value === page ? "active" : ""} aria-current={value === page ? "page" : undefined} disabled={disabled || value === page} onClick={() => onPageChange(value)}>{value}</button>
        ))}
      </div>
      <button type="button" className="button button-soft" disabled={disabled || !hasNext} onClick={() => onPageChange(page + 1)}>다음</button>
    </nav>
  );
}

