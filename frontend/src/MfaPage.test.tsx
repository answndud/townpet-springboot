import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import MfaPage from "./MfaPage";

afterEach(() => vi.unstubAllGlobals());

describe("MfaPage", () => {
  it("enrolls a moderator factor and displays recovery codes once", async () => {
    vi.stubGlobal("fetch", vi.fn((input) => {
      const path = String(input);
      if (path.endsWith("/api/v1/auth/csrf")) {
        document.cookie = "XSRF-TOKEN=test-token";
        return Promise.resolve(new Response(JSON.stringify({ token: "test-token" }), { status: 200 }));
      }
      if (path.endsWith("/api/v1/auth/mfa/enrollment")) {
        return Promise.resolve(new Response(JSON.stringify({ secret: "JBSWY3DPEHPK3PXP", otpauthUri: "otpauth://totp/TownPet:test", expiresAt: "2026-08-20T01:00:00Z" }), { status: 200 }));
      }
      if (path.endsWith("/api/v1/auth/mfa/enrollment/confirm")) {
        return Promise.resolve(new Response(JSON.stringify({ recoveryCodes: ["ABCD1234EFGH5678"] }), { status: 200 }));
      }
      return Promise.resolve(new Response(JSON.stringify({ detail: "Unexpected request" }), { status: 500 }));
    }));

    render(<MemoryRouter initialEntries={["/admin/mfa?mode=enroll&next=%2Fadmin"]}><MfaPage /></MemoryRouter>);

    expect(await screen.findByText("JBSWY3DPEHPK3PXP")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("인증 코드"), { target: { value: "123456" } });
    fireEvent.submit(screen.getByRole("button", { name: "확인" }).closest("form")!);
    await waitFor(() => expect(screen.getByText("ABCD1234EFGH5678")).toBeInTheDocument());
  });
});
