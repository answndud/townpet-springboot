import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: ".",
  testMatch: [
    "auth-parity.spec.ts",
    "publication-parity.spec.ts",
    "feed-parity.spec.ts",
    "publication-management.spec.ts",
    "comment-management.spec.ts",
    "reaction-management.spec.ts",
    "bookmark-management.spec.ts",
    "relationship-management.spec.ts",
    "parity-shell.spec.ts",
  ],
  fullyParallel: false,
  workers: 1,
  reporter: "list",
  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 5"] } },
  ],
});
