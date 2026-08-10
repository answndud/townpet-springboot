export type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  traceId?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
};

export type Session = {
  memberId: string;
  expiresAt: string;
};

export type Neighborhood = {
  id: string;
  slug: string;
  name: string;
};

export type Pet = {
  id: string;
  name: string;
  species: string;
};

export type Member = {
  id: string;
  nickname: string;
  bio: string | null;
  neighborhoodId: string | null;
  pets: Pet[];
};

export type OnboardingInput = {
  bio: string;
  neighborhoodId: string;
  pets: Array<{ name: string; species: string }>;
};

export type PublicationScope = "LOCAL" | "GLOBAL";

export type Publication = {
  id: string;
  type: "FREE_BOARD";
  title: string;
  body: string;
  scope: PublicationScope;
  authorId: string;
  neighborhoodId: string | null;
  lifecycle: "ACTIVE" | "DELETED";
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type Comment = {
  id: string;
  publicationId: string;
  authorId: string;
  body: string;
  lifecycle: "ACTIVE" | "DELETED";
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type Reaction = {
  active: boolean;
  count: number;
};

export type Bookmark = {
  active: boolean;
};

export type CreatePublicationInput = {
  title: string;
  body: string;
  scope: PublicationScope;
  neighborhoodId?: string;
};

export type EditPublicationInput = CreatePublicationInput & {
  version: number;
};

export type CreateCommentInput = {
  body: string;
};

export type FeedPage = {
  items: Publication[];
  page: {
    nextCursor: string | null;
    hasNext: boolean;
  };
};

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: ProblemDetail,
  ) {
    super(problem.detail ?? problem.title ?? "TownPet API request failed");
    this.name = "ApiError";
  }
}

/** Transport seam for the generated OpenAPI client; feature code never builds API URLs directly. */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method?.toUpperCase() ?? "GET";
  const csrf = document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
  const response = await fetch(path, {
    credentials: "include",
    ...init,
    headers: {
      accept: "application/json",
      ...(method !== "GET" && method !== "HEAD" && csrf
        ? { "X-XSRF-TOKEN": decodeURIComponent(csrf) }
        : {}),
      ...init?.headers,
    },
  });
  const body = await response.text();

  if (!response.ok) {
    let problem: ProblemDetail = {};
    if (body) {
      try {
        problem = JSON.parse(body) as ProblemDetail;
      } catch {
        problem = { detail: body };
      }
    }
    throw new ApiError(response.status, problem);
  }

  return (body ? JSON.parse(body) : undefined) as T;
}

export async function getCsrfToken(): Promise<string> {
  const response = await apiFetch<{ token: string }>("/api/v1/auth/csrf");
  return response.token;
}

async function mutate<T>(path: string, init: RequestInit): Promise<T> {
  await getCsrfToken();
  return apiFetch<T>(path, init);
}

const jsonHeaders = { "content-type": "application/json" };

export const authApi = {
  login(email: string, password: string) {
    return mutate<Session>("/api/v1/auth/sessions", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ email: email.trim(), password }),
    });
  },
  logout() {
    return mutate<void>("/api/v1/auth/sessions/current", { method: "DELETE" });
  },
  requestPasswordReset(email: string) {
    return mutate<void>("/api/v1/auth/password-resets", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ email: email.trim() }),
    });
  },
  confirmPasswordReset(token: string, newPassword: string) {
    return mutate<void>("/api/v1/auth/password-resets/confirmations", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ token: token.trim(), newPassword }),
    });
  },
  requestEmailVerification(email: string) {
    return mutate<void>("/api/v1/auth/email-verifications", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ email: email.trim() }),
    });
  },
  confirmEmailVerification(token: string) {
    return mutate<void>("/api/v1/auth/email-verifications/confirmations", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ token: token.trim() }),
    });
  },
};

export const memberApi = {
  current(signal?: AbortSignal) {
    return apiFetch<Member>("/api/v1/members/me", { signal });
  },
  updateOnboarding(input: OnboardingInput) {
    return mutate<Member>("/api/v1/members/me/onboarding", {
      method: "PUT",
      headers: jsonHeaders,
      body: JSON.stringify(input),
    });
  },
};

export const catalogApi = {
  neighborhoods(signal?: AbortSignal) {
    return apiFetch<Neighborhood[]>("/api/v1/catalog/neighborhoods", { signal });
  },
};

export const publicationApi = {
  create(input: CreatePublicationInput) {
    return mutate<Publication>("/api/v1/publications", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify(input),
    });
  },
  detail(publicationId: string, signal?: AbortSignal) {
    return apiFetch<Publication>(`/api/v1/publications/${encodeURIComponent(publicationId)}`, {
      signal,
    });
  },
  edit(publicationId: string, input: EditPublicationInput) {
    return mutate<Publication>(`/api/v1/publications/${encodeURIComponent(publicationId)}`, {
      method: "PUT",
      headers: jsonHeaders,
      body: JSON.stringify(input),
    });
  },
  delete(publicationId: string, version: number) {
    return mutate<void>(`/api/v1/publications/${encodeURIComponent(publicationId)}`, {
      method: "DELETE",
      headers: jsonHeaders,
      body: JSON.stringify({ version }),
    });
  },
  comments(publicationId: string, signal?: AbortSignal) {
    return apiFetch<{ items: Comment[] }>(
      `/api/v1/publications/${encodeURIComponent(publicationId)}/comments`,
      { signal },
    );
  },
  createComment(publicationId: string, input: CreateCommentInput) {
    return mutate<Comment>(
      `/api/v1/publications/${encodeURIComponent(publicationId)}/comments`,
      {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify(input),
      },
    );
  },
  deleteComment(publicationId: string, commentId: string, version: number) {
    return mutate<void>(
      `/api/v1/publications/${encodeURIComponent(publicationId)}/comments/${encodeURIComponent(commentId)}`,
      {
        method: "DELETE",
        headers: jsonHeaders,
        body: JSON.stringify({ version }),
      },
    );
  },
  reaction(publicationId: string, signal?: AbortSignal) {
    return apiFetch<Reaction>(
      `/api/v1/publications/${encodeURIComponent(publicationId)}/reaction`,
      { signal },
    );
  },
  setReaction(publicationId: string, active: boolean) {
    return mutate<Reaction>(
      `/api/v1/publications/${encodeURIComponent(publicationId)}/reaction`,
      {
        method: "PUT",
        headers: jsonHeaders,
        body: JSON.stringify({ active }),
      },
    );
  },
  bookmark(publicationId: string, signal?: AbortSignal) {
    return apiFetch<Bookmark>(
      `/api/v1/publications/${encodeURIComponent(publicationId)}/bookmark`,
      { signal },
    );
  },
  setBookmark(publicationId: string, active: boolean) {
    return mutate<Bookmark>(
      `/api/v1/publications/${encodeURIComponent(publicationId)}/bookmark`,
      {
        method: "PUT",
        headers: jsonHeaders,
        body: JSON.stringify({ active }),
      },
    );
  },
  feed({
    audience = "VIEWER",
    cursor,
    limit = 20,
    signal,
  }: {
    audience?: "GLOBAL" | "VIEWER";
    cursor?: string;
    limit?: number;
    signal?: AbortSignal;
  } = {}) {
    const search = new URLSearchParams({ audience, limit: String(limit) });
    if (cursor) search.set("cursor", cursor);
    return apiFetch<FeedPage>(`/api/v1/feed?${search}`, { signal });
  },
};
