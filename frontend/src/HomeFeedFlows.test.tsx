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
  it("opens on HOT and switches to the existing all-posts view", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) return Promise.resolve(response({ title: "Unauthorized" }, 401));
      if (path.includes("/api/v1/discovery/popular")) {
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

    expect(await screen.findByRole("heading", { name: "HOT 글" })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "추천받은 산책 이야기" })).toBeInTheDocument();
    expect(await screen.findByText("추천 7")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /^전체$/ }));
    expect(await screen.findByRole("heading", { name: "최신 전체글" })).toBeInTheDocument();
    fireEvent.change(screen.getByRole("combobox", { name: "검색 위치" }), { target: { value: "TITLE" } });
    fireEvent.change(screen.getByRole("textbox", { name: "검색어" }), { target: { value: "산책" } });
    fireEvent.click(screen.getByRole("button", { name: "검색" }));
    expect(fetchMock).toHaveBeenLastCalledWith(
      "/api/v1/discovery?limit=20&query=%EC%82%B0%EC%B1%85&searchField=TITLE",
      expect.objectContaining({ credentials: "include" }),
    );
    expect(await screen.findByRole("heading", { name: "최신 전체글" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "HOT" }));
    expect(await screen.findByRole("heading", { name: "HOT 글" })).toBeInTheDocument();
  });
});
