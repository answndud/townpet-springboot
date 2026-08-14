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

function publication(id: string, title: string, scope: "GLOBAL" | "LOCAL" = "GLOBAL") {
  return {
    id,
    kind: "PUBLICATION",
    type: "FREE_BOARD",
    title,
    body: `${title} 본문입니다.`,
    scope,
    authorId: "00000000-0000-4000-8000-000000000201",
    neighborhoodId: scope === "LOCAL" ? "00000000-0000-4000-8000-000000000101" : null,
    status: "ACTIVE",
    lifecycle: "ACTIVE",
    createdAt: "2026-08-10T09:00:00Z",
    updatedAt: "2026-08-10T09:00:00Z",
    version: 0,
    href: `/posts/${id}`,
  };
}

afterEach(() => {
  window.localStorage.clear();
  vi.unstubAllGlobals();
});

describe("Publication feed journeys", () => {
  it("loads the public feed and appends the stable-cursor page", async () => {
    const firstId = "0198f342-13d7-7000-8000-000000000001";
    const secondId = "0198f342-13d7-7000-8000-000000000002";
    const fetchMock = vi.fn<typeof fetch>((input) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) return Promise.resolve(response({ detail: "Unauthorized" }, 401));
      if (path.includes("cursor=next-page")) {
        return Promise.resolve(
          response({
            items: [publication(secondId, "두 번째 전체 글")],
            page: { nextCursor: null, hasNext: false },
          }),
        );
      }
      return Promise.resolve(
        response({
          items: [publication(firstId, "첫 번째 전체 글")],
          page: { nextCursor: "next-page", hasNext: true },
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter initialEntries={["/?view=all"]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "전체글" })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "첫 번째 전체 글" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "다음 페이지" }));
    expect(await screen.findByRole("heading", { name: "두 번째 전체 글" })).toBeInTheDocument();
    expect(screen.getAllByRole("article")).toHaveLength(1);
    expect(fetchMock).toHaveBeenLastCalledWith(
      "/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL&cursor=next-page",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("requires a current member for the member feed route", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      if (String(input).endsWith("/api/v1/members/me")) {
        return Promise.resolve(response({ title: "Unauthorized" }, 401));
      }
      return Promise.resolve(response({ items: [], page: { nextCursor: null, hasNext: false } }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter initialEntries={["/feed"]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "로그인" })).toBeInTheDocument();
  });

  it("resolves a direct page URL through the cursor chain", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) return Promise.resolve(response({ detail: "Unauthorized" }, 401));
      if (path.includes("cursor=page-two")) {
        return Promise.resolve(response({ items: [publication("0198f342-13d7-7000-8000-000000000012", "두 번째 페이지")], page: { nextCursor: null, hasNext: false } }));
      }
      return Promise.resolve(response({ items: [publication("0198f342-13d7-7000-8000-000000000011", "첫 번째 페이지")], page: { nextCursor: "page-two", hasNext: true } }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter initialEntries={["/?view=all&page=2"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "두 번째 페이지" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "첫 번째 페이지" })).not.toBeInTheDocument();
    expect(fetchMock).toHaveBeenLastCalledWith(
      "/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL&cursor=page-two",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("passes the URL search term to the feed and exposes the reset action", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      if (String(input).endsWith("/api/v1/members/me")) return Promise.resolve(response({ detail: "Unauthorized" }, 401));
      return Promise.resolve(response({ items: [publication("0198f342-13d7-7000-8000-000000000003", "산책 장소")], page: { nextCursor: null, hasNext: false } }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter initialEntries={["/?view=all&q=산책"]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "산책 장소" })).toBeInTheDocument();
    expect(screen.getByDisplayValue("산책")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "검색어 초기화" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL&query=%EC%82%B0%EC%B1%85",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("executes a guest search when opened from a direct URL", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      if (String(input).includes("/api/v1/feed?")) {
        return Promise.resolve(response({ items: [publication("0198f342-13d7-7000-8000-000000000004", "산책 검색 결과")], page: { nextCursor: null, hasNext: false } }));
      }
      return Promise.resolve(response({ title: "Unauthorized" }, 401));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter initialEntries={["/search/guest?q=산책"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "산책 검색 결과" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL&query=%EC%82%B0%EC%B1%85",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("keeps the legacy feed independent from animal board navigation", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      if (String(input).endsWith("/api/v1/members/me")) return Promise.resolve(response({ detail: "Unauthorized" }, 401));
      return Promise.resolve(response({ items: [], page: { nextCursor: null, hasNext: false } }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter initialEntries={["/?view=all"]}><App /></MemoryRouter>);

    await screen.findByRole("heading", { name: "전체글" });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("passes a board type from the URL to the feed request", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      if (String(input).endsWith("/api/v1/members/me")) return Promise.resolve(response({ detail: "Unauthorized" }, 401));
      return Promise.resolve(response({ items: [], page: { nextCursor: null, hasNext: false } }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter initialEntries={["/?view=all&q=질문"]}><App /></MemoryRouter>);

    await screen.findByRole("heading", { name: "전체글" });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL&query=%EC%A7%88%EB%AC%B8",
      expect.objectContaining({ credentials: "include" }),
    );
  });

});
