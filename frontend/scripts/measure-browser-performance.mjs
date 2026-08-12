import { chromium } from "@playwright/test";

const baseUrl = process.env.TOWNPET_BROWSER_BASE_URL ?? "http://127.0.0.1:4173";
const routes = ["/", "/feed/guest", "/marketplace", "/lost-found", "/guides?q=%EC%82%B0%EC%B1%85", "/gatherings", "/care"];
const settleWaitMs = Number(process.env.TOWNPET_BROWSER_SETTLE_MS ?? 500);

const browser = await chromium.launch({ headless: true });
const results = [];

for (const path of routes) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  await page.addInitScript(() => {
    window.__townpetVitals = { cls: 0, lcp: 0, inp: 0 };
    new PerformanceObserver((list) => {
      const entries = list.getEntries();
      const latest = entries.at(-1);
      if (latest) window.__townpetVitals.lcp = latest.startTime;
    }).observe({ type: "largest-contentful-paint", buffered: true });
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (!entry.hadRecentInput) window.__townpetVitals.cls += entry.value;
      }
    }).observe({ type: "layout-shift", buffered: true });
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        window.__townpetVitals.inp = Math.max(window.__townpetVitals.inp, entry.duration);
      }
    }).observe({ type: "event", buffered: true, durationThreshold: 16 });
  });

  const startedAt = performance.now();
  await page.goto(new URL(path, baseUrl).toString(), { waitUntil: "networkidle" });
  await page.keyboard.press("Tab");
  await page.waitForTimeout(settleWaitMs);
  const browserMetrics = await page.evaluate(() => {
    const navigation = performance.getEntriesByType("navigation")[0];
    const paint = performance.getEntriesByType("paint");
    return {
      ...window.__townpetVitals,
      fcp: paint.find((entry) => entry.name === "first-contentful-paint")?.startTime ?? null,
      domContentLoaded: navigation?.domContentLoadedEventEnd ?? null,
      loadEventEnd: navigation?.loadEventEnd ?? null,
    };
  });
  results.push({ path, routeSettleMs: Math.round((performance.now() - startedAt) * 100) / 100, ...browserMetrics });
  await page.close();
}

await browser.close();
console.log(JSON.stringify({ baseUrl, viewport: "1280x900", settleWaitMs, measuredAt: new Date().toISOString(), routes: results }, null, 2));
