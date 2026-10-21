import { describe, expect, it } from "vitest";
import { formatDateCompactTime, formatDateMediumTime, formatDateTime, formatDateTimeLong } from "./date";

describe("formatDateCompactTime", () => {
  it("omits the year for dates within the current year", () => {
    const now = new Date(2026, 9, 21);
    const value = new Date(2026, 8, 16, 19, 25).toISOString();

    const formatted = formatDateCompactTime(value, now);
    expect(formatted).not.toContain("2026");
    expect(formatted).toContain("19:25");
    expect(formatted).not.toMatch(/오전|오후/);
  });

  it("includes the year for dates from another year", () => {
    const now = new Date(2026, 9, 21);
    const value = new Date(2025, 7, 10, 9, 0).toISOString();

    expect(formatDateCompactTime(value, now)).toContain("2025");
  });

  it("uses 24-hour time in full and medium date formats", () => {
    const value = new Date(2026, 8, 16, 19, 25).toISOString();

    for (const formatted of [formatDateTime(value), formatDateTimeLong(value), formatDateMediumTime(value)]) {
      expect(formatted).toContain("19:25");
      expect(formatted).not.toMatch(/오전|오후/);
    }
  });
});
