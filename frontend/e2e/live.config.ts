import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: ".",
  testMatch: "**/*.spec.ts",
  testIgnore: ["desktop-visual.spec.ts"],
  fullyParallel: false,
  workers: 1,
  reporter: [["list"], ["html", { outputFolder: "../build/e2e-report", open: "never" }]],
  use: {
    baseURL: process.env.TOWNPET_E2E_BASE_URL ?? "http://127.0.0.1:5173",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 5"] } },
  ],
});
