import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

afterEach(() => vi.unstubAllGlobals());

describe("Animal community journeys", () => {

  it("offers a write action and available engagement data on the common-board feed", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>((input) => {
      if (String(input).endsWith("/api/v1/members/me")) {
        return Promise.resolve(new Response(JSON.stringify({ detail: "Unauthorized" }), { status: 401 }));
      }
      return Promise.resolve(new Response(JSON.stringify({
        items: [{ id: "common-post", kind: "PUBLICATION", type: "FREE_BOARD", title: "동네 산책 코스", body: "내용", authorId: null, neighborhoodId: null, status: "ACTIVE", lifecycle: "ACTIVE", createdAt: "2026-08-12T08:00:00Z", updatedAt: "2026-08-12T08:00:00Z", version: 0, href: "/posts/common-post", recommendationCount: 3 }],
        page: { nextCursor: null, hasNext: false },
      }), { status: 200, headers: { "content-type": "application/json" } }));
    }));

    render(<MemoryRouter initialEntries={["/boards/all"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "동네 산책 코스" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "글쓰기" })).toHaveAttribute("href", "/posts/new");
    expect(screen.getByText("추천 3")).toBeInTheDocument();
  });

  it("does not present an anonymous publication as an operations-team post", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>((input) => {
      if (String(input).endsWith("/api/v1/members/me")) {
        return Promise.resolve(new Response(JSON.stringify({ detail: "Unauthorized" }), { status: 401 }));
      }
      return Promise.resolve(new Response(JSON.stringify({
        items: [{ id: "guest-post", kind: "PUBLICATION", type: "FREE_BOARD", title: "익명 질문", body: "내용", authorId: null, neighborhoodId: null, animalCode: "DOG", status: "ACTIVE", lifecycle: "ACTIVE", createdAt: "2026-08-12T08:00:00Z", updatedAt: "2026-08-12T08:00:00Z", version: 0, href: "/posts/guest-post" }],
        page: { nextCursor: null, hasNext: false }, animalCode: "dog", board: "free",
      }), { status: 200, headers: { "content-type": "application/json" } }));
    }));

    render(<MemoryRouter initialEntries={["/animals/dog/free"]}><App /></MemoryRouter>);

    expect(await screen.findByText("익명 이웃")).toBeInTheDocument();
    expect(screen.queryByText("TownPet 운영팀")).not.toBeInTheDocument();
  });

  it("opens the canonical marketplace from its common-board alias", async () => {
    const fetchMock = vi.fn<typeof fetch>(input => String(input).endsWith("/api/v1/members/me")
      ? Promise.resolve(new Response(JSON.stringify({detail:"Unauthorized"}), {status:401}))
      : Promise.resolve(new Response(JSON.stringify([{id:"market-post",title:"강아지 용품 거래",description:"판매 설명",kind:"SELL",status:"AVAILABLE",priceKrw:12000,createdAt:"2026-08-12T08:00:00Z",version:0}]), {status:200,headers:{"content-type":"application/json"}})));
    vi.stubGlobal("fetch",fetchMock);
    render(<MemoryRouter initialEntries={["/boards/marketplace?q=용품&searchField=TITLE"]}><App /></MemoryRouter>);
    expect(await screen.findByRole("heading",{name:"동네 거래"})).toBeInTheDocument();
    expect(await screen.findByRole("heading",{name:"강아지 용품 거래"})).toBeInTheDocument();
    expect(screen.getByText("12,000원")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/marketplace/listings?limit=30&q=%EC%9A%A9%ED%92%88&searchField=TITLE",expect.objectContaining({credentials:"include"}));
  });
});
