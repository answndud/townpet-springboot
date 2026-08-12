type ApiTiming = { path: string; status: number; durationMs: number };

export type TownPetPerformanceSnapshot = {
  api: ApiTiming[];
  route: Array<{ path: string; durationMs: number }>;
  lcpMs: number | null;
  cls: number;
};

declare global {
  interface Window {
    __townpetPerformance?: TownPetPerformanceSnapshot;
  }
}

const isBrowser = typeof window !== "undefined";
const snapshot: TownPetPerformanceSnapshot = {
  api: [],
  route: [],
  lcpMs: null,
  cls: 0,
};
let observersInstalled = false;

if (isBrowser && import.meta.env.DEV) {
  window.__townpetPerformance = snapshot;
}

function getSnapshot() {
  if (isBrowser) window.__townpetPerformance = snapshot;
  return snapshot;
}

function normalizePath(path: string) {
  return path.split("?", 1)[0].replace(/\/[0-9a-f-]{16,}/gi, "/:id");
}

export function recordApiTiming(path: string, status: number, durationMs: number) {
  if (!isBrowser || !import.meta.env.DEV) return;
  const timings = getSnapshot().api;
  timings.push({ path: normalizePath(path), status, durationMs: Math.round(durationMs) });
  if (timings.length > 100) timings.splice(0, timings.length - 100);
}

export function recordRouteTiming(path: string, durationMs: number) {
  if (!isBrowser || !import.meta.env.DEV) return;
  const timings = getSnapshot().route;
  timings.push({ path: normalizePath(path), durationMs: Math.round(durationMs) });
  if (timings.length > 50) timings.splice(0, timings.length - 50);
}

export function installPerformanceObservers() {
  if (!isBrowser || !import.meta.env.DEV) return;
  getSnapshot();
  if (typeof PerformanceObserver === "undefined") return;
  if (observersInstalled) return;
  observersInstalled = true;
  try {
    new PerformanceObserver((list) => {
      const latest = list.getEntries().at(-1);
      if (latest) snapshot.lcpMs = Math.round(latest.startTime);
    }).observe({ type: "largest-contentful-paint", buffered: true });
  } catch {
    // Older browsers may not expose LCP.
  }
  try {
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries() as Array<PerformanceEntry & { hadRecentInput?: boolean; value?: number }>) {
        if (!entry.hadRecentInput) snapshot.cls += entry.value ?? 0;
      }
    }).observe({ type: "layout-shift", buffered: true });
  } catch {
    // Older browsers may not expose layout shift entries.
  }
}
