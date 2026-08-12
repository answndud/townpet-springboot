import { useCallback, useEffect, useRef, useState } from "react";

export function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === "AbortError"
    || error instanceof Error && error.name === "AbortError";
}

export function useAbortableRequest<T>(
  request: (signal: AbortSignal) => Promise<T>,
  dependencies: readonly unknown[],
) {
  const latestRequest = useRef(request);
  const controllerRef = useRef<AbortController | null>(null);
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);

  latestRequest.current = request;

  const execute = useCallback(() => {
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    setData(null);
    setLoading(true);
    setError(null);
    latestRequest.current(controller.signal)
      .then((nextData) => {
        if (controllerRef.current === controller) setData(nextData);
      })
      .catch((requestError: unknown) => {
        if (isAbortError(requestError) || controllerRef.current !== controller) return;
        setError(requestError);
      })
      .finally(() => {
        if (controllerRef.current === controller) setLoading(false);
      });
  }, []);

  useEffect(() => {
    execute();
    return () => controllerRef.current?.abort();
    // The caller supplies the request's query inputs explicitly.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [execute, ...dependencies]);

  return { data, error, loading, retry: execute };
}
