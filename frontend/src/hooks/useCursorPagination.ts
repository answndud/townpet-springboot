import { useEffect, useRef, useState } from "react";

export type CursorPage<T> = {
  items: T[];
  page: { nextCursor: string | null; hasNext: boolean };
};

type CursorCache<T> = {
  pages: Map<number, CursorPage<T>>;
  cursors: Map<number, string | null>;
};

type CursorPaginationOptions<T> = {
  enabled?: boolean;
  page: number;
  pageSize: number;
  queryKey: string;
  fetchPage: (cursor: string | undefined, signal: AbortSignal) => Promise<CursorPage<T>>;
};

export function useCursorPagination<T>({ enabled = true, page, pageSize, queryKey, fetchPage }: CursorPaginationOptions<T>) {
  const caches = useRef(new Map<string, CursorCache<T>>());
  const fetchPageRef = useRef(fetchPage);
  fetchPageRef.current = fetchPage;
  const [result, setResult] = useState<{
    items: T[];
    loading: boolean;
    error: string | null;
    hasNext: boolean;
  }>({ items: [], loading: true, error: null, hasNext: false });

  useEffect(() => {
    if (!enabled) {
      setResult({ items: [], loading: false, error: null, hasNext: false });
      return;
    }
    const controller = new AbortController();
    const cache: CursorCache<T> = caches.current.get(queryKey) ?? {
      pages: new Map<number, CursorPage<T>>(),
      cursors: new Map<number, string | null>([[1, null]]),
    };
    caches.current.set(queryKey, cache);
    const targetPage = Math.max(1, page);
    const cached = cache.pages.get(targetPage);
    if (cached) {
      setResult({ items: cached.items, loading: false, error: null, hasNext: cached.page.hasNext });
      return () => controller.abort();
    }

    let active = true;
    setResult({ items: [], loading: true, error: null, hasNext: false });
    void (async () => {
      try {
        let startPage = 1;
        for (const knownPage of cache.pages.keys()) {
          if (knownPage < targetPage && knownPage > startPage) startPage = knownPage;
        }
        let cursor = cache.cursors.get(startPage) ?? null;
        let currentPage: CursorPage<T> | null = null;
        for (let requestedPage = startPage; requestedPage <= targetPage; requestedPage += 1) {
          if (cache.pages.has(requestedPage)) {
            currentPage = cache.pages.get(requestedPage) ?? null;
            cursor = cache.cursors.get(requestedPage + 1) ?? null;
            continue;
          }
          currentPage = await fetchPageRef.current(cursor ?? undefined, controller.signal);
          cache.pages.set(requestedPage, currentPage);
          cache.cursors.set(requestedPage + 1, currentPage.page.nextCursor);
          cursor = currentPage.page.nextCursor;
          if (!currentPage.page.hasNext && requestedPage < targetPage) break;
        }
        if (!active || controller.signal.aborted) return;
        if (!currentPage || !cache.pages.has(targetPage)) {
          setResult({ items: [], loading: false, error: "요청한 페이지를 찾을 수 없습니다.", hasNext: false });
          return;
        }
        setResult({ items: currentPage.items, loading: false, error: null, hasNext: currentPage.page.hasNext });
      } catch (error) {
        if (!active || controller.signal.aborted) return;
        setResult((current) => ({ ...current, loading: false, error: error instanceof Error ? error.message : "게시글을 불러오지 못했습니다." }));
      }
    })();

    return () => {
      active = false;
      controller.abort();
    };
  }, [enabled, page, pageSize, queryKey]);

  return result;
}
