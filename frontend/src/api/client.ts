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

export type PublicMember = Member;

export type OnboardingInput = {
  bio: string;
  neighborhoodId: string;
  pets: Array<{ name: string; species: string }>;
};

export type LostFoundAlertKind = "LOST" | "FOUND";
export type LostFoundAlertStatus = "ACTIVE" | "RESOLVED" | "CLOSED";
export type LostFoundLocation = { latitude: number; longitude: number };
export type LostFoundAlert = {
  id: string;
  reporterMemberId: string;
  kind: LostFoundAlertKind;
  status: LostFoundAlertStatus;
  title: string;
  description: string;
  lastSeenAt: string;
  approximateLocation: LostFoundLocation;
  resolutionOutcome: string | null;
  closeReason: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};
export type LostFoundSighting = {
  id: string;
  alertId: string;
  reporterMemberId: string;
  seenAt: string;
  description: string;
  approximateLocation: LostFoundLocation;
  createdAt: string;
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

export type Relationship = {
  following: boolean;
  blocking: boolean;
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

export type MarketplaceListingKind = "SELL" | "RENT" | "SHARE";
export type MarketplaceListingStatus = "AVAILABLE" | "RESERVED" | "COMPLETED" | "CANCELLED";
export type MarketplaceListing = {
  id: string;
  ownerMemberId: string;
  kind: MarketplaceListingKind;
  status: MarketplaceListingStatus;
  title: string;
  description: string;
  priceKrw: number | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type CreateMarketplaceListingInput = {
  kind: MarketplaceListingKind;
  title: string;
  description: string;
  priceKrw: number | null;
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

/** Small transport seam; feature code keeps endpoint details in one client module. */
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
  profile(memberId: string, signal?: AbortSignal) {
    return apiFetch<PublicMember>(`/api/v1/members/${encodeURIComponent(memberId)}`, { signal });
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
  relationship(memberId: string, signal?: AbortSignal) {
    return apiFetch<Relationship>(
      `/api/v1/members/${encodeURIComponent(memberId)}/relationship`,
      { signal },
    );
  },
  setRelationship(memberId: string, following: boolean, blocking: boolean) {
    return mutate<Relationship>(
      `/api/v1/members/${encodeURIComponent(memberId)}/relationship`,
      {
        method: "PUT",
        headers: jsonHeaders,
        body: JSON.stringify({ following, blocking }),
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

export const marketplaceApi = {
  list(kind?: MarketplaceListingKind, limit = 20, signal?: AbortSignal) {
    const search = new URLSearchParams({ limit: String(limit) });
    if (kind) search.set("kind", kind);
    return apiFetch<MarketplaceListing[]>(`/api/v1/marketplace/listings?${search}`, { signal });
  },
  detail(listingId: string, signal?: AbortSignal) {
    return apiFetch<MarketplaceListing>(
      `/api/v1/marketplace/listings/${encodeURIComponent(listingId)}`,
      { signal },
    );
  },
  create(input: CreateMarketplaceListingInput) {
    return mutate<MarketplaceListing>('/api/v1/marketplace/listings', {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify(input),
    });
  },
  update(listingId: string, input: CreateMarketplaceListingInput & { version: number }) {
    return mutate<MarketplaceListing>(
      `/api/v1/marketplace/listings/${encodeURIComponent(listingId)}`,
      { method: "PATCH", headers: jsonHeaders, body: JSON.stringify(input) },
    );
  },
  changeStatus(listingId: string, status: MarketplaceListingStatus, version: number) {
    return mutate<MarketplaceListing>(
      `/api/v1/marketplace/listings/${encodeURIComponent(listingId)}/status`,
      {
        method: "PATCH",
        headers: jsonHeaders,
        body: JSON.stringify({ status, version }),
      },
    );
  },
};

export const lostFoundApi = {
  list({ kind, latitude, longitude, radiusMeters, limit = 20, signal }: {
    kind?: LostFoundAlertKind;
    latitude?: number;
    longitude?: number;
    radiusMeters?: number;
    limit?: number;
    signal?: AbortSignal;
  } = {}) {
    const search = new URLSearchParams({ limit: String(limit) });
    if (kind) search.set("kind", kind);
    if (latitude !== undefined) search.set("latitude", String(latitude));
    if (longitude !== undefined) search.set("longitude", String(longitude));
    if (radiusMeters !== undefined) search.set("radiusMeters", String(radiusMeters));
    return apiFetch<LostFoundAlert[]>(`/api/v1/lost-found/alerts?${search}`, { signal });
  },
  detail(alertId: string, signal?: AbortSignal) {
    return apiFetch<LostFoundAlert>(`/api/v1/lost-found/alerts/${encodeURIComponent(alertId)}`, { signal });
  },
  create(input: {
    kind: LostFoundAlertKind;
    title: string;
    description: string;
    lastSeenAt: string;
    latitude: number;
    longitude: number;
  }) {
    return mutate<LostFoundAlert>("/api/v1/lost-found/alerts", { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) });
  },
  changeStatus(alertId: string, input: { status: LostFoundAlertStatus; resolutionOutcome?: string; closeReason?: string; reopenReason?: string }) {
    return mutate<LostFoundAlert>(`/api/v1/lost-found/alerts/${encodeURIComponent(alertId)}/status`, { method: "PATCH", headers: jsonHeaders, body: JSON.stringify(input) });
  },
  sightings(alertId: string, limit = 20, signal?: AbortSignal) {
    return apiFetch<LostFoundSighting[]>(`/api/v1/lost-found/alerts/${encodeURIComponent(alertId)}/sightings?limit=${limit}`, { signal });
  },
  createSighting(alertId: string, input: { seenAt: string; description: string; latitude: number; longitude: number; exactLatitude?: number; exactLongitude?: number }) {
    return mutate<LostFoundSighting>(`/api/v1/lost-found/alerts/${encodeURIComponent(alertId)}/sightings`, { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) });
  },
  exactLocation(sightingId: string, signal?: AbortSignal) {
    return apiFetch<{ sightingId: string; latitude: number; longitude: number }>(`/api/v1/lost-found/sightings/${encodeURIComponent(sightingId)}/exact-location`, { signal });
  },
};
