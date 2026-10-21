import { describe, expect, it } from "vitest";
import { formatDateCompactTime } from "./date";

describe("formatDateCompactTime", () => {
  it("omits the year for dates within the current year", () => {
    const now = new Date(2026, 9, 21);
    const value = new Date(2026, 7, 10, 9, 0).toISOString();

    expect(formatDateCompactTime(value, now)).not.toContain("2026");
  });

  it("includes the year for dates from another year", () => {
    const now = new Date(2026, 9, 21);
    const value = new Date(2025, 7, 10, 9, 0).toISOString();

    expect(formatDateCompactTime(value, now)).toContain("2025");
  });
});
