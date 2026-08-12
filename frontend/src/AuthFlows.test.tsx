import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
  it("keeps the password reset request response account-neutral", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(response({ token: "csrf-token" }))
      .mockResolvedValueOnce(response(undefined, 202));
    vi.stubGlobal("fetch", fetchMock);
    document.cookie = "XSRF-TOKEN=csrf-token; path=/";

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
      await screen.findByText("입력한 주소의 계정을 확인한 뒤 필요한 경우 재설정 메일을 보냈습니다."),
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

  it("renders onboarding data fetched in parallel", async () => {
    const fetchMock = vi.fn<typeof fetch>((input) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) {
        return Promise.resolve(
          response({
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
          }),
        );
      }
      return Promise.resolve(
        response([
          {
            id: "00000000-0000-4000-8000-000000000101",
            slug: "seoul-mapogu",
            name: "서울 마포구",
          },
        ]),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter initialEntries={["/onboarding"]}>
        <App />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole("heading", { name: "내 동네와 반려동물 설정" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "서울 마포구" })).toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3));
  });
});
