import { describe, expect, it } from "vitest";
import { normalizeFeedPage } from "./api/client";

describe("CursorPagination", () => {

  it("normalizes missing feed metadata without throwing", () => {
    expect(normalizeFeedPage(undefined)).toEqual({
      items: [],
      page: { nextCursor: null, hasNext: false },
    });
    expect(normalizeFeedPage({ items: [], nextCursor: "legacy-cursor", hasNext: true })).toEqual({
      items: [],
      page: { nextCursor: "legacy-cursor", hasNext: true },
    });
  });
});
