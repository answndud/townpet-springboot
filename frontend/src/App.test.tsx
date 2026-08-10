import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import App from "./App";

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

    expect(screen.getByRole("heading", { name: "TownPet 로그인" })).toBeInTheDocument();
    expect(screen.getByLabelText("이메일")).toHaveAttribute("type", "email");
    expect(screen.getByLabelText("비밀번호")).toHaveAttribute("type", "password");
  });
});
