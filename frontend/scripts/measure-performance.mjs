import { performance } from "node:perf_hooks";

const baseUrl = process.env.TOWNPET_BASE_URL ?? "http://localhost:5173";
const repeat = Number(process.env.TOWNPET_PERF_REPEAT ?? 3);
const routeBudgetMs = 100;
const apiBudgetMs = 250;
const routes = ["/", "/feed/guest", "/marketplace", "/lost-found", "/guides?q=%EC%82%B0%EC%B1%85", "/gatherings", "/care"];
const apiRequests = [
  "/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL",
  "/api/v1/marketplace/listings?limit=30",
  "/api/v1/lost-found/alerts?limit=20",
  "/api/v1/local-resources?query=%EC%82%B0%EC%B1%85",
  "/api/v1/gatherings",
  "/api/v1/care/requests",
];

async function measure(path) {
  const startedAt = performance.now();
  const response = await fetch(new URL(path, baseUrl), { cache: "no-store" });
  const body = await response.arrayBuffer();
  const durationMs = performance.now() - startedAt;
  return {
    path,
    status: response.status,
    durationMs: Math.round(durationMs * 100) / 100,
    bytes: body.byteLength,
  };
}

async function measureGroup(paths) {
  const results = [];
  for (const path of paths) {
    const samples = [];
    for (let index = 0; index < repeat; index += 1) samples.push(await measure(path));
    const sortedSamples = samples.map((sample) => sample.durationMs).sort((a, b) => a - b);
    results.push({
      path,
      samples,
      medianMs: sortedSamples[Math.floor(sortedSamples.length / 2)],
      p75Ms: sortedSamples[Math.max(0, Math.ceil(sortedSamples.length * 0.75) - 1)],
    });
  }
  return results;
}

const [routeResults, apiResults] = await Promise.all([measureGroup(routes), measureGroup(apiRequests)]);

console.log(JSON.stringify({ baseUrl, repeat, budgets: { routeMedianMs: routeBudgetMs, apiMedianMs: apiBudgetMs }, measuredAt: new Date().toISOString(), routes: routeResults, api: apiResults }, null, 2));

const serverFailures = [...routeResults, ...apiResults].filter((result) => result.samples.some((sample) => sample.status >= 500));
const budgetFailures = [
  ...routeResults.filter((result) => result.medianMs > routeBudgetMs),
  ...apiResults.filter((result) => result.medianMs > apiBudgetMs),
];
if (serverFailures.length || budgetFailures.length) {
  if (serverFailures.length) console.error(`Unexpected server failures: ${serverFailures.map(({ path }) => path).join(", ")}`);
  if (budgetFailures.length) console.error(`Performance budget failures: ${budgetFailures.map(({ path, medianMs }) => `${path}=${medianMs}ms`).join(", ")}`);
  process.exitCode = 1;
}
