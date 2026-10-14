import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
  it("searches a named region without requesting or exposing exact coordinates", async () => {
    const fetchMock = vi.fn<typeof fetch>(input => String(input).endsWith("/api/v1/members/me")
      ? Promise.resolve(response({detail:"Unauthorized"},401)) : Promise.resolve(response([alert])));
    vi.stubGlobal("fetch",fetchMock);
    render(<MemoryRouter initialEntries={["/lost-found"]}><App /></MemoryRouter>);
    expect(await screen.findByRole("heading",{name:"Mango를 찾습니다"})).toBeInTheDocument();
    expect(screen.queryByLabelText("위도")).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole("combobox",{name:"동네 선택"}),{target:{value:"마포·서대문"}});
    fireEvent.change(screen.getByRole("combobox",{name:"검색 거리"}),{target:{value:"2000"}});
    fireEvent.click(screen.getByRole("button",{name:"근처 검색"}));
    await waitFor(()=>expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("latitude=37.555&longitude=126.915&radiusMeters=2000"),expect.anything()));
  });
});
