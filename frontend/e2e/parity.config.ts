export const parityTargets = {
  legacy: process.env.LEGACY_BASE_URL ?? "http://localhost:3000",
  spring: process.env.SPRING_BASE_URL ?? "http://localhost:5173",
} as const;

export const parityFixture = "migration/fixtures/logical-fixture.yaml";

export const parityRules = {
  normalize: ["uuid", "timestamp", "signed-url", "traceId", "requestId"],
  compare: "normalized-json",
  requiredActors: ["guest", "member", "staff"],
} as const;
