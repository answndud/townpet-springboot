import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

afterEach(() => vi.unstubAllGlobals());

describe("TownPet Vite shell", () => {
  it("renders the legacy home identity and primary journeys", () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "우리 동네 반려생활 정보" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "전체 피드" })).toHaveAttribute("href", "/feed/guest");
    expect(screen.getByRole("link", { name: "로그인" })).toHaveAttribute("href", "/login");
  });

  it("renders the identity vertical slice login form", () => {
    render(
      <MemoryRouter initialEntries={["/login"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "로그인" })).toBeInTheDocument();
    expect(screen.getByLabelText("이메일")).toHaveAttribute("type", "email");
    expect(screen.getByLabelText("비밀번호")).toHaveAttribute("type", "password");
    expect(screen.getAllByRole("link", { name: "비밀번호 재설정" })).toHaveLength(2);
    for (const link of screen.getAllByRole("link", { name: "비밀번호 재설정" })) {
      expect(link).toHaveAttribute("href", "/password/reset");
    }
    expect(screen.queryByRole("link", { name: "회원가입" })).not.toBeInTheDocument();
    expect(screen.queryByText(/카카오|네이버/)).not.toBeInTheDocument();
  });

  it("renders the reset and verification routes", () => {
    const { unmount } = render(
      <MemoryRouter initialEntries={["/password/reset"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "비밀번호 재설정" })).toBeInTheDocument();
    unmount();

    render(
      <MemoryRouter initialEntries={["/verify-email"]}>
        <App />
      </MemoryRouter>,
    );
    expect(screen.getByRole("heading", { name: "이메일 인증" })).toBeInTheDocument();
  });

  it("redirects an unauthenticated public profile URL to credentials login", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve(new Response(JSON.stringify({ detail: "Unauthorized" }), { status: 401 }))),
    );

    render(
      <MemoryRouter initialEntries={["/users/00000000-0000-4000-8000-000000000202"]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "로그인" })).toBeInTheDocument();
  });
});
