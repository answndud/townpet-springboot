import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

const PUBLICATION_ID = "0198f342-13d7-7000-8000-000000000001";

function response(body: unknown, status = 200) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: body === undefined ? undefined : { "content-type": "application/json" },
  });
}

function publication() {
  return {
    id: PUBLICATION_ID,
    type: "FREE_BOARD",
    title: "저녁 산책 정보를 나눠요",
    body: "공원 입구가 한산해요.",
    scope: "GLOBAL",
    authorId: "00000000-0000-4000-8000-000000000201",
    neighborhoodId: null,
    lifecycle: "ACTIVE",
    createdAt: "2026-08-10T09:00:00Z",
    updatedAt: "2026-08-10T09:00:00Z",
    version: 0,
  };
}

afterEach(() => {
  document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("Publication journeys", () => {
  it("creates a member free-board publication and opens its direct detail route", async () => {
    const fetchMock = vi.fn<typeof fetch>((input, init) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) {
        return Promise.resolve(
          response({
            id: "00000000-0000-4000-8000-000000000201",
            nickname: "demo-member-1",
            bio: null,
            neighborhoodId: "00000000-0000-4000-8000-000000000101",
            pets: [],
          }),
        );
      }
      if (path.endsWith("/api/v1/catalog/neighborhoods")) {
        return Promise.resolve(
          response([
            {
              id: "00000000-0000-4000-8000-000000000101",
              slug: "seoul-mapogu",
              name: "서울 마포구",
            },
          ]),
        );
      }
      if (path.endsWith("/api/v1/auth/csrf")) {
        return Promise.resolve(response({ token: "csrf-token" }));
      }
      if (path.endsWith("/api/v1/publications") && init?.method === "POST") {
        return Promise.resolve(response(publication(), 201));
      }
      if (path.endsWith(`/api/v1/publications/${PUBLICATION_ID}`)) {
        return Promise.resolve(response(publication()));
      }
      return Promise.reject(new Error(`Unexpected request: ${path}`));
    });
    vi.stubGlobal("fetch", fetchMock);
    document.cookie = "XSRF-TOKEN=csrf-token; path=/";

    render(
      <MemoryRouter initialEntries={["/posts/new"]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "새 글 작성" })).toBeInTheDocument();
    expect(screen.getByText("서울 마포구")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("제목"), {
      target: { value: "저녁 산책 정보를 나눠요" },
    });
    fireEvent.change(screen.getByLabelText("본문"), {
      target: { value: "공원 입구가 한산해요." },
    });
    fireEvent.click(screen.getByRole("radio", { name: /내 동네/ }));
    fireEvent.click(screen.getByRole("button", { name: "등록" }));

    expect(
      await screen.findByRole("heading", { name: "저녁 산책 정보를 나눠요" }),
    ).toBeInTheDocument();
    expect(screen.getByText("공원 입구가 한산해요.")).toBeInTheDocument();
    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([input, init]) =>
          String(input).endsWith("/api/v1/publications") && init?.method === "POST",
      );
      expect(createCall?.[1]?.body).toBe(
        JSON.stringify({
          title: "저녁 산책 정보를 나눠요",
          body: "공원 입구가 한산해요.",
          scope: "LOCAL",
          neighborhoodId: "00000000-0000-4000-8000-000000000101",
        }),
      );
    });
  });

  it("shows the public not-found state without requiring a session", async () => {
    vi.stubGlobal("fetch", vi.fn<typeof fetch>(() => Promise.resolve(response({ title: "Not Found" }, 404))));

    render(
      <MemoryRouter initialEntries={[`/posts/${PUBLICATION_ID}`]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "존재하지 않거나 삭제된 게시글입니다.",
    );
  });

  it("lets the author edit and delete with the loaded publication version", async () => {
    let currentPublication = publication();
    const fetchMock = vi.fn<typeof fetch>((input, init) => {
      const path = String(input);
      if (path.endsWith("/api/v1/members/me")) {
        return Promise.resolve(
          response({
            id: currentPublication.authorId,
            nickname: "demo-member-1",
            bio: null,
            neighborhoodId: "00000000-0000-4000-8000-000000000101",
            pets: [],
          }),
        );
      }
      if (path.endsWith("/api/v1/catalog/neighborhoods")) {
        return Promise.resolve(
          response([
            {
              id: "00000000-0000-4000-8000-000000000101",
              slug: "seoul-mapogu",
              name: "서울 마포구",
            },
          ]),
        );
      }
      if (path.endsWith("/api/v1/auth/csrf")) {
        return Promise.resolve(response({ token: "csrf-token" }));
      }
      if (path.endsWith(`/api/v1/publications/${PUBLICATION_ID}`) && init?.method === "PUT") {
        currentPublication = {
          ...currentPublication,
          title: "수정한 산책 정보",
          body: "수정한 공원 정보입니다.",
          version: 1,
        };
        return Promise.resolve(response(currentPublication));
      }
      if (path.endsWith(`/api/v1/publications/${PUBLICATION_ID}`) && init?.method === "DELETE") {
        return Promise.resolve(response(undefined, 204));
      }
      if (path.endsWith(`/api/v1/publications/${PUBLICATION_ID}`)) {
        return Promise.resolve(response(currentPublication));
      }
      if (path.includes("/api/v1/feed?")) {
        return Promise.resolve(response({ items: [], page: { nextCursor: null, hasNext: false } }));
      }
      return Promise.reject(new Error(`Unexpected request: ${path}`));
    });
    vi.stubGlobal("fetch", fetchMock);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    document.cookie = "XSRF-TOKEN=csrf-token; path=/";

    render(
      <MemoryRouter initialEntries={[`/posts/${PUBLICATION_ID}/edit`]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "게시글 수정" })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("제목"), { target: { value: "수정한 산책 정보" } });
    fireEvent.change(screen.getByLabelText("본문"), { target: { value: "수정한 공원 정보입니다." } });
    fireEvent.click(screen.getByRole("button", { name: "변경 사항 저장" }));

    expect(await screen.findByRole("heading", { name: "수정한 산책 정보" })).toBeInTheDocument();
    const editCall = fetchMock.mock.calls.find(([, init]) => init?.method === "PUT");
    expect(editCall?.[1]?.body).toBe(
      JSON.stringify({
        title: "수정한 산책 정보",
        body: "수정한 공원 정보입니다.",
        scope: "GLOBAL",
        version: 0,
      }),
    );

    fireEvent.click(await screen.findByRole("button", { name: "삭제" }));
    expect(await screen.findByRole("heading", { name: "내 동네와 전체 새 글" })).toBeInTheDocument();
    const deleteCall = fetchMock.mock.calls.find(([, init]) => init?.method === "DELETE");
    expect(deleteCall?.[1]?.body).toBe(JSON.stringify({ version: 1 }));
  });
});
