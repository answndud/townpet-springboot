import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

afterEach(() => vi.unstubAllGlobals());

describe("Local care journeys", () => {
  it("keeps a direct guide search URL functional", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      expect(String(input)).toBe("/api/v1/local-resources?query=%EC%82%B0%EC%B1%85");
      return Promise.resolve(new Response(JSON.stringify([{
        id: "00000000-0000-4000-8000-000000005001",
        kind: "LOCAL_GUIDE",
        title: "산책 코스 안내",
        summary: "가까운 산책 정보를 확인하세요.",
        content: "본문",
        sourceName: "TownPet",
        sourceUrl: null,
        updatedAt: "2026-08-10T09:00:00Z",
      }]), { status: 200, headers: { "content-type": "application/json" } }));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter initialEntries={["/guides?q=산책"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "산책 코스 안내" })).toBeInTheDocument();
    expect(screen.getByDisplayValue("산책")).toBeInTheDocument();
  });
});
