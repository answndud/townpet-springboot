import { readdirSync, statSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { gzipSync } from "node:zlib";
import { join } from "node:path";

const assetsDir = join(process.cwd(), "dist", "assets");
const assets = readdirSync(assetsDir).map((name) => ({
  name,
  path: join(assetsDir, name),
}));
const entry = assets.find(({ name }) => /^index-[^/]+\.js$/.test(name));
const css = assets.find(({ name }) => /^index-[^/]+\.css$/.test(name));

if (!entry || !css) {
  throw new Error("Bundle budget could not find the hashed entry JS and CSS assets.");
}

const entryBytes = statSync(entry.path).size;
const entryGzipBytes = gzipSync(await readFile(entry.path)).length;
const cssBytes = statSync(css.path).size;
const budgets = [
  ["entry JS", entryBytes, 320_000],
  ["entry JS gzip", entryGzipBytes, 100_000],
  ["entry CSS", cssBytes, 51_000],
];

for (const [label, actual, limit] of budgets) {
  console.log(`[bundle-budget] ${label}: ${actual} bytes / ${limit} bytes`);
  if (actual > limit) throw new Error(`${label} exceeds its ${limit}-byte budget.`);
}
