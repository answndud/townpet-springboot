import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

function response(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

const alert = {
  id: "0198f342-13d7-7000-8000-000000000401",
  reporterMemberId: "00000000-0000-4000-8000-000000000201",
  kind: "LOST",
  status: "ACTIVE",
  title: "Mango를 찾습니다",
  description: "공원 근처에서 마지막으로 봤어요.",
  lastSeenAt: "2026-08-10T09:00:00Z",
  approximateLocation: { latitude: 37.55, longitude: 126.91 },
  resolutionOutcome: null,
  closeReason: null,
  createdAt: "2026-08-10T09:00:00Z",
  updatedAt: "2026-08-10T09:00:00Z",
  version: 0,
};

afterEach(() => vi.unstubAllGlobals());

describe("LostFound journeys", () => {
  it("loads public alerts and opens the sighting flow", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      const path = String(input);
      if (path.includes("/api/v1/lost-found/alerts?") && !path.includes("/sightings")) return Promise.resolve(response([alert]));
      if (path.endsWith("/api/v1/lost-found/alerts/" + alert.id)) return Promise.resolve(response(alert));
      if (path.includes("/sightings?")) return Promise.resolve(response([]));
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

    render(<MemoryRouter initialEntries={["/lost-found"]}><App /></MemoryRouter>);
    expect(await screen.findByRole("heading", { name: "Mango를 찾습니다" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("link", { name: /Mango를 찾습니다/ }));
    expect(await screen.findByRole("heading", { name: "Mango를 찾습니다" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "목격 제보" })).toHaveAttribute("href", `/lost-found/${alert.id}/sightings/new`);
  });
});
