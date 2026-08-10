import { describe, expect, it } from "vitest";
import { equivalentPayloads, normalizePayload } from "./normalize";

describe("differential payload normalization", () => {
  it("ignores volatile IDs, timestamps, signed URLs, and tracing fields", () => {
    const legacy = {
      id: "00000000-0000-4000-8000-000000000001",
      createdAt: "2026-01-01T00:00:00.000Z",
      imageUrl: "https://cdn.example.test/a?X-Amz-Signature=legacy",
      traceId: "legacy-trace",
      title: "same meaning",
    };
    const spring = {
      title: "same meaning",
      id: "00000000-0000-4000-8000-000000000002",
      createdAt: "2026-01-01T00:00:00.999Z",
      imageUrl: "https://cdn.example.test/b?signature=spring",
      traceId: "spring-trace",
    };

    expect(equivalentPayloads(legacy, spring)).toBe(true);
  });

  it("keeps business differences visible", () => {
    expect(normalizePayload({ title: "legacy" })).not.toEqual(normalizePayload({ title: "spring" }));
  });
});
