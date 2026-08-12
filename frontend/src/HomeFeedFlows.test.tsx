import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

function response(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("Home feed journeys", () => {
  it("switches the first page between all posts and recommendation-ranked posts", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) return Promise.resolve(response({ title: "Unauthorized" }, 401));
      if (path.includes("/api/v1/feed/popular")) {
        return Promise.resolve(response({
          items: [{ id: "0198f342-13d7-7000-8000-000000000005", title: "추천받은 산책 이야기", body: "인기글 본문입니다.", createdAt: "2026-08-12T08:00:00Z", recommendationCount: 7, rank: 1 }],
        }));
      }
      return Promise.resolve(response({
        items: [{
          id: "0198f342-13d7-7000-8000-000000000006",
          kind: "PUBLICATION",
          type: "FREE_BOARD",
          title: "최신 전체글",
          body: "최신 전체글 본문입니다.",
          scope: "GLOBAL",
          authorId: "00000000-0000-4000-8000-000000000201",
          neighborhoodId: null,
          status: "ACTIVE",
          lifecycle: "ACTIVE",
          createdAt: "2026-08-12T08:00:00Z",
          updatedAt: "2026-08-12T08:00:00Z",
          version: 0,
          href: "/posts/0198f342-13d7-7000-8000-000000000006",
        }],
        page: { nextCursor: null, hasNext: false },
      }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter initialEntries={["/"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "전체글" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "최신 전체글" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "인기글" }));
    expect(await screen.findByRole("heading", { name: "인기글" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "추천받은 산책 이야기" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "최신 전체글" })).not.toBeInTheDocument();
    expect(screen.getByText("추천 7")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "전체글" }));
    expect(await screen.findByRole("heading", { name: "최신 전체글" })).toBeInTheDocument();
  });
});
