import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

function response(body: unknown, status = 200) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: body === undefined ? undefined : { "content-type": "application/json" },
  });
}

afterEach(() => {
  document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  vi.unstubAllGlobals();
});

describe("Credentials journeys", () => {
  it("does not redirect a successful login back to the credentials route", async () => {
    const member = {
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
    };
    const fetchMock = vi.fn<typeof fetch>((input, init) => {
      const path = String(input);
      if (path.endsWith("/api/v1/auth/sessions") && init?.method === "POST") {
        return Promise.resolve(response({ memberId: member.id, expiresAt: "2026-08-13T00:00:00Z", role: "MEMBER" }));
      }
      if (path.endsWith("/api/v1/members/me")) return Promise.resolve(response(member));
      return Promise.resolve(response({ items: [], page: { nextCursor: null, hasNext: false } }));
    });
    vi.stubGlobal("fetch", fetchMock);
    document.cookie = "XSRF-TOKEN=test-token; path=/";

    render(
      <MemoryRouter initialEntries={["/login?next=/profile"]}>
        <App />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("이메일"), { target: { value: "demo-member-1@townpet.local" } });
    fireEvent.change(screen.getByLabelText("비밀번호"), { target: { value: "townpet-demo-123!" } });
    fireEvent.click(screen.getByRole("button", { name: "이메일로 로그인" }));

    expect(await screen.findByRole("heading", { name: "demo-member-1님의 프로필" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "내 작성글" })).toHaveAttribute("href", "/my-posts");
    expect(screen.queryByRole("heading", { name: "로그인" })).not.toBeInTheDocument();
  });

  it("keeps the password reset request response account-neutral", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(response({ token: "csrf-token" }))
      .mockResolvedValueOnce(response(undefined, 202));
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter initialEntries={["/password/reset"]}>
        <App />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("이메일"), {
      target: { value: "unknown@example.com" },
    });
    fireEvent.click(screen.getByRole("button", { name: "재설정 메일 요청" }));

    expect(
      await screen.findByText("계정이 있으면 재설정 메일을 보냅니다."),
    ).toBeInTheDocument();
    expect(fetchMock).toHaveBeenLastCalledWith(
      "/api/v1/auth/password-resets",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "content-type": "application/json",
          "X-XSRF-TOKEN": "csrf-token",
        }),
      }),
    );
  });
});
