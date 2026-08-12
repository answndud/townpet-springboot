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

const listing = {
  id: "0198f342-13d7-7000-8000-000000000301",
  ownerMemberId: "00000000-0000-4000-8000-000000000201",
  kind: "SELL",
  status: "AVAILABLE",
  title: "강아지 이동장",
  description: "깨끗하게 사용했어요.",
  priceKrw: 15000,
  createdAt: "2026-08-10T09:00:00Z",
  updatedAt: "2026-08-10T09:00:00Z",
  version: 0,
};

afterEach(() => vi.unstubAllGlobals());

describe("Marketplace journeys", () => {
  it("loads the public listing and creates a new listing", async () => {
    const fetchMock = vi.fn<typeof fetch>((input, init) => {
      const path = String(input);
      if (path.includes("/api/v1/marketplace/listings?") && !init?.method) {
        return Promise.resolve(response([listing]));
      }
      if (path.endsWith("/api/v1/auth/csrf")) return Promise.resolve(response({ token: "csrf" }));
      if (path.endsWith("/api/v1/marketplace/listings") && init?.method === "POST") {
        return Promise.resolve(response({ ...listing, title: "새 이동장" }, 201));
      }
      if (path.includes("/api/v1/marketplace/listings/")) return Promise.resolve(response(listing));
      if (path.endsWith("/api/v1/members/me")) return Promise.resolve(response({
        id: "00000000-0000-4000-8000-000000000201",
        nickname: "demo-member-1",
        role: "MEMBER",
        bio: null,
        neighborhoodId: null,
        pets: [],
        showPublicPosts: true,
        showPublicComments: true,
        showPublicPets: true,
        showPublicReactions: true,
      }));
      return Promise.resolve(response({}));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter initialEntries={["/marketplace"]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "강아지 이동장" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("link", { name: "글 올리기" }));
    expect(await screen.findByRole("heading", { name: "새 거래 글" })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("거래 유형"), { target: { value: "SELL" } });
    fireEvent.change(screen.getByLabelText("제목"), { target: { value: "새 이동장" } });
    fireEvent.change(screen.getByLabelText("설명"), { target: { value: "새 상품 설명" } });
    fireEvent.change(screen.getByLabelText("가격(원)"), { target: { value: "12000" } });
    fireEvent.click(screen.getByRole("button", { name: "등록" }));

    expect(await screen.findByRole("heading", { name: "강아지 이동장" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/marketplace/listings",
      expect.objectContaining({ method: "POST" }),
    );
  });
});
