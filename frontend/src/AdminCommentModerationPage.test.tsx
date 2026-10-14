import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

const COMMENT_ID = "0198f342-13d7-7000-8000-000000000901";
const PUBLICATION_ID = "0198f342-13d7-7000-8000-000000000001";

function response(body: unknown, status = 200) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: body === undefined ? undefined : { "content-type": "application/json" },
  });
}

afterEach(() => {
  document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("Admin comment moderation", () => {
  it("finds and removes a comment without a report, retaining the action reason", async () => {
    let removed = false;
    const comment = () => ({
      id: COMMENT_ID,
      publicationId: PUBLICATION_ID,
      parentCommentId: null,
      authorType: "GUEST",
      lifecycle: removed ? "MODERATOR_DELETED" : "ACTIVE",
      body: "신고 없이도 확인할 수 있는 댓글입니다.",
      createdAt: "2026-08-10T09:00:00Z",
      updatedAt: "2026-08-10T09:00:00Z",
      version: removed ? 1 : 0,
    });
    const fetchMock = vi.fn<typeof fetch>((input, init) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) {
        return Promise.resolve(response({ id: "moderator-id", nickname: "운영자", role: "MODERATOR" }));
      }
      if (path.endsWith("/api/v1/auth/csrf")) return Promise.resolve(response({ token: "csrf-token" }));
      if (path.startsWith("/api/admin/comments?") && init?.method !== "POST") {
        return Promise.resolve(response([comment()]));
      }
      if (path.endsWith(`/api/admin/comments/${COMMENT_ID}/moderation`) && init?.method === "POST") {
        removed = true;
        return Promise.resolve(response(comment()));
      }
      return Promise.reject(new Error(`Unexpected request: ${path}`));
    });
    vi.stubGlobal("fetch", fetchMock);
    document.cookie = "XSRF-TOKEN=csrf-token; path=/";

    render(
      <MemoryRouter initialEntries={["/admin/comments"]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByText("신고 없이도 확인할 수 있는 댓글입니다.")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("운영 조치 사유"), { target: { value: "운영 기준 위반" } });
    fireEvent.click(screen.getByRole("button", { name: "운영자 삭제" }));

    expect(await screen.findByText("댓글을 운영자 조치로 삭제했습니다.")).toBeInTheDocument();
    await waitFor(() => expect(screen.getAllByText("운영자 삭제").length).toBeGreaterThan(1));
    const moderationCall = fetchMock.mock.calls.find(
      ([input, init]) => String(input).endsWith(`/api/admin/comments/${COMMENT_ID}/moderation`) && init?.method === "POST",
    );
    expect(moderationCall?.[1]?.body).toBe(JSON.stringify({ action: "DELETE", reason: "운영 기준 위반" }));
  });
});
