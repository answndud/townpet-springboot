import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

afterEach(() => vi.unstubAllGlobals());

describe("Animal community journeys", () => {
  it("loads one animal community and exposes its internal board tabs", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) {
        return Promise.resolve(new Response(JSON.stringify({ detail: "Unauthorized" }), { status: 401 }));
      }
      return Promise.resolve(new Response(JSON.stringify({
        items: [{ id: "dog-post", kind: "PUBLICATION", type: "FREE_BOARD", title: "강아지 산책 질문", body: "강아지와 걷기 좋은 길을 알려 주세요.", scope: "GLOBAL", authorId: "member", neighborhoodId: null, animalCode: "DOG", status: "ACTIVE", lifecycle: "ACTIVE", createdAt: "2026-08-12T08:00:00Z", updatedAt: "2026-08-12T08:00:00Z", version: 0, href: "/posts/dog-post" }],
        page: { nextCursor: null, hasNext: false },
        animalCode: "dog",
        board: "questions",
      }), { status: 200, headers: { "content-type": "application/json" } }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter initialEntries={["/animals/dog/questions"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "강아지 게시판" })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "강아지 산책 질문" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "입양" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "반려동물 자랑" })).toHaveAttribute("href", "/animals/dog/showcase");
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/communities/dog/feed?audience=VIEWER&board=questions&limit=20&scope=ALL",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("does not present an anonymous publication as an operations-team post", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>((input) => {
      if (String(input).endsWith("/api/v1/members/me")) {
        return Promise.resolve(new Response(JSON.stringify({ detail: "Unauthorized" }), { status: 401 }));
      }
      return Promise.resolve(new Response(JSON.stringify({
        items: [{ id: "guest-post", kind: "PUBLICATION", type: "FREE_BOARD", title: "익명 질문", body: "내용", scope: "GLOBAL", authorId: null, neighborhoodId: null, animalCode: "DOG", status: "ACTIVE", lifecycle: "ACTIVE", createdAt: "2026-08-12T08:00:00Z", updatedAt: "2026-08-12T08:00:00Z", version: 0, href: "/posts/guest-post" }],
        page: { nextCursor: null, hasNext: false }, animalCode: "dog", board: "free",
      }), { status: 200, headers: { "content-type": "application/json" } }));
    }));

    render(<MemoryRouter initialEntries={["/animals/dog/free"]}><App /></MemoryRouter>);

    expect(await screen.findByText("익명 이웃")).toBeInTheDocument();
    expect(screen.queryByText("TownPet 운영팀")).not.toBeInTheDocument();
  });

  it("loads a common board outside the animal community route", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) {
        return Promise.resolve(new Response(JSON.stringify({ detail: "Unauthorized" }), { status: 401 }));
      }
      return Promise.resolve(new Response(JSON.stringify({
        items: [{ id: "market-post", kind: "MARKETPLACE", type: "MARKETPLACE", title: "강아지 용품 거래", body: "공통 거래 게시판 글입니다.", scope: "GLOBAL", authorId: "member", neighborhoodId: null, animalCode: null, status: "AVAILABLE", lifecycle: "AVAILABLE", createdAt: "2026-08-12T08:00:00Z", updatedAt: "2026-08-12T08:00:00Z", version: 0, href: "/marketplace/market-post" }],
        page: { nextCursor: null, hasNext: false },
      }), { status: 200, headers: { "content-type": "application/json" } }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter initialEntries={["/boards/marketplace"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "공통게시판" })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "강아지 용품 거래" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "봉사" })).toHaveAttribute("href", "/boards/volunteer");
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/boards/marketplace/feed?audience=VIEWER&limit=20&scope=ALL",
      expect.objectContaining({ credentials: "include" }),
    );
  });
});
