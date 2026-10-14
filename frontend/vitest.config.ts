import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "jsdom",
    maxWorkers: 2,
    setupFiles: ["./src/test-setup.ts"],
    include: ["src/**/*.test.ts", "src/**/*.test.tsx"],
  },
});
