import { recordApiTiming } from "../utils/performance";

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
  role: "MEMBER" | "MODERATOR";
};

export type Neighborhood = {
  id: string;
  slug: string;
  name: string;
};
export type Breed = { code: string; species: "DOG" | "CAT" | "OTHER"; name: string; description: string };
export type AnimalInterest = { code: string; group: string; label: string; sortOrder: number };

export type Pet = {
  id: string;
  name: string;
  species: string;
};

export type Member = {
  id: string;
  nickname: string;
  role: "MEMBER" | "MODERATOR";
  bio: string | null;
  neighborhoodId: string | null;
  pets: Pet[];
  showPublicPosts: boolean;
  showPublicComments: boolean;
  showPublicPets: boolean;
  showPublicReactions: boolean;
};

export type PublicMember = Omit<Member, "role">;
export type PublicMemberPublication = { id: string; title: string; body: string; scope: PublicationScope; createdAt: string; updatedAt: string };
export type PublicMemberComment = { id: string; publicationId: string; body: string; createdAt: string };
export type PublicMemberReaction = { publicationId: string; type: string; createdAt: string };

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
  type: "FREE_BOARD" | "QA_QUESTION" | "PET_SHOWCASE" | "PRODUCT_REVIEW";
  title: string;
  body: string;
  scope: PublicationScope;
  authorId: string;
  neighborhoodId: string | null;
  animalInterestCode?: string | null;
  animalCommunityCodes?: string[];
  lifecycle: "ACTIVE" | "DELETED";
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type FeedItem = {
  id: string;
  kind: string;
  type: string;
  title: string;
  body: string;
  scope: PublicationScope;
  authorId: string | null;
  neighborhoodId: string | null;
  animalInterestCode?: string | null;
  animalCode?: string | null;
  status: string;
  lifecycle: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  href: string;
};

export type Comment = {
  id: string;
  publicationId: string;
  authorId: string;
  parentCommentId: string | null;
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
export type PublicationStats = { viewCount: number };
export type MediaUpload = { id: string; uploadUrl: string; objectKey: string; checksumSha256: string; contentType: string; byteSize: number; status: string; publicationId: string | null; expiresAt: string; version: number };
export type CareRequest = { id: string; requesterMemberId: string; title: string; description: string; location: string; startsAt: string; endsAt: string; rewardHint: string | null; status: "OPEN" | "MATCHED" | "CANCELLED" | "EXPIRED"; createdAt: string; updatedAt: string; version: number };
export type CareApplication = { id: string; requestId: string; applicantMemberId: string; message: string; status: "PENDING" | "ACCEPTED" | "DECLINED" | "WITHDRAWN"; createdAt: string; updatedAt: string; version: number };
export type CareAssignment = { id: string; requestId: string; caregiverMemberId: string; status: "MATCHED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED_BY_REQUESTER" | "CANCELLED_BY_CAREGIVER" | "ABORTED"; createdAt: string; updatedAt: string; version: number };
export type VolunteerOpportunity = { id: string; publisherMemberId: string; title: string; description: string; organization: string; location: string; startsAt: string; capacity: number; status: string; createdAt: string; updatedAt: string; version: number };
export type HospitalReview = { id: string; authorMemberId: string; hospitalName: string; address: string; rating: number; body: string; createdAt: string; updatedAt: string; version: number };

export type Relationship = {
  following: boolean;
  blocking: boolean;
};

export type LocalResourceKind = "LOCAL_GUIDE" | "WELFARE" | "CARE";
export type LocalResource = {
  id: string;
  kind: LocalResourceKind;
  title: string;
  summary: string;
  content: string;
  sourceName: string;
  sourceUrl: string | null;
  updatedAt: string;
};
export type Gathering = { id: string; hostMemberId: string; title: string; description: string; location: string; startsAt: string; capacity: number; participantCount: number; status: "ACTIVE" | "CANCELLED"; joined: boolean; version: number };
export type AdoptionListing = { id: string; publisherMemberId: string; neighborhoodId: string | null; title: string; description: string; species: string; breed: string | null; status: string; createdAt: string; updatedAt: string; version: number };
export type TrustReportReason = "SPAM" | "ABUSE" | "PRIVACY" | "ILLEGAL" | "OTHER";
export type Notification = { id: string; type: string; title: string; body: string; readAt: string | null; createdAt: string };
export type PolicyDocument = { key: string; title: string; body: string; updatedAt: string };

export type CreatePublicationInput = {
  title: string;
  body: string;
  type?: Publication["type"];
  scope: PublicationScope;
  neighborhoodId?: string;
  animalInterestCode?: string | null;
  animalCommunityCodes?: string[];
};

export type EditPublicationInput = CreatePublicationInput & {
  version: number;
};

export type CreateCommentInput = {
  body: string;
  parentCommentId?: string;
};

export type FeedPage = {
  items: FeedItem[];
  page: {
    nextCursor: string | null;
    hasNext: boolean;
  };
};

export type CommunityFeedPage = FeedPage & {
  animalCode: string;
  board: string;
};

export type MarketplaceListingKind = "SELL" | "RENT" | "SHARE" | "GROUP_BUY";
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
  const startedAt = typeof performance === "undefined" ? 0 : performance.now();
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
    recordApiTiming(path, response.status, startedAt ? performance.now() - startedAt : 0);
    throw new ApiError(response.status, problem);
  }

  const result = (body ? JSON.parse(body) : undefined) as T;
  recordApiTiming(path, response.status, startedAt ? performance.now() - startedAt : 0);
  return result;
}

type CachedGetEntry = { value: unknown; expiresAt: number };
const cachedGetEntries = new Map<string, CachedGetEntry>();
const cachedGetRequests = new Map<string, Promise<unknown>>();

function abortError() {
  return new DOMException("The operation was aborted.", "AbortError");
}

function withAbortSignal<T>(request: Promise<T>, signal?: AbortSignal) {
  if (!signal) return request;
  return new Promise<T>((resolve, reject) => {
    if (signal.aborted) {
      reject(abortError());
      return;
    }
    const cleanup = () => signal.removeEventListener("abort", handleAbort);
    const handleAbort = () => {
      cleanup();
      reject(abortError());
    };
    signal.addEventListener("abort", handleAbort, { once: true });
    request.then(
      (value) => { cleanup(); resolve(value); },
      (error: unknown) => { cleanup(); reject(error); },
    );
  });
}

function cachedGet<T>(key: string, path: string, signal: AbortSignal | undefined, ttlMs: number) {
  if (import.meta.env.MODE === "test") return apiFetch<T>(path, { signal });
  const now = Date.now();
  const cached = cachedGetEntries.get(key);
  if (cached && cached.expiresAt > now) {
    return withAbortSignal(Promise.resolve(cached.value as T), signal);
  }

  let request = cachedGetRequests.get(key) as Promise<T> | undefined;
  if (!request) {
    request = apiFetch<T>(path).then((value) => {
      cachedGetEntries.set(key, { value, expiresAt: Date.now() + ttlMs });
      return value;
    });
    cachedGetRequests.set(key, request);
    void request.then(
      () => { if (cachedGetRequests.get(key) === request) cachedGetRequests.delete(key); },
      () => { if (cachedGetRequests.get(key) === request) cachedGetRequests.delete(key); },
    );
  }
  return withAbortSignal(request, signal);
}

function invalidateCachedGet(key: string) {
  cachedGetEntries.delete(key);
}

let csrfRequest: Promise<string> | null = null;

function csrfCookie() {
  return document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
}

export function getCsrfToken(): Promise<string> {
  const cookieToken = csrfCookie();
  if (cookieToken) return Promise.resolve(decodeURIComponent(cookieToken));
  if (!csrfRequest) {
    csrfRequest = apiFetch<{ token: string }>("/api/v1/auth/csrf")
      .then((response) => response.token)
      .finally(() => { csrfRequest = null; });
  }
  return csrfRequest;
}

export async function apiMutate<T>(path: string, init: RequestInit): Promise<T> {
  const csrfToken = await getCsrfToken();
  return apiFetch<T>(path, {
    ...init,
    headers: {
      ...(init.headers ?? {}),
      "X-XSRF-TOKEN": csrfToken,
    },
  });
}

function currentMember(signal?: AbortSignal) {
  return apiFetch<Member>("/api/v1/members/me", { signal });
}

async function mutate<T>(path: string, init: RequestInit): Promise<T> {
  return apiMutate<T>(path, init);
}

const jsonHeaders = { "content-type": "application/json" };

export const authApi = {
  async login(email: string, password: string) {
    const session = await mutate<Session>("/api/v1/auth/sessions", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ email: email.trim(), password }),
    });
    window.dispatchEvent(new Event("townpet:auth-change"));
    return session;
  },
  async logout() {
    const result = await mutate<void>("/api/v1/auth/sessions/current", { method: "DELETE" });
    window.dispatchEvent(new Event("townpet:auth-change"));
    return result;
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
  current: currentMember,
  profile(memberId: string, signal?: AbortSignal) {
    return apiFetch<PublicMember>(`/api/v1/members/${encodeURIComponent(memberId)}`, { signal });
  },
  publicPublications(memberId: string, signal?: AbortSignal) {
    return apiFetch<PublicMemberPublication[]>(`/api/v1/members/${encodeURIComponent(memberId)}/publications`, { signal });
  },
  publicComments(memberId: string, signal?: AbortSignal) {
    return apiFetch<PublicMemberComment[]>(`/api/v1/members/${encodeURIComponent(memberId)}/comments`, { signal });
  },
  publicReactions(memberId: string, signal?: AbortSignal) {
    return apiFetch<PublicMemberReaction[]>(`/api/v1/members/${encodeURIComponent(memberId)}/reactions`, { signal });
  },
  myPosts(signal?: AbortSignal) { return apiFetch<Publication[]>("/api/v1/publications/mine", { signal }); },
  bookmarks(signal?: AbortSignal) {
    return cachedGet<string[]>("member:bookmarks", "/api/v1/members/me/bookmarks", signal, 15_000);
  },
  animalInterests(signal?: AbortSignal) {
    return apiFetch<string[]>("/api/v1/members/me/preferences/animal-interests", { signal });
  },
  updateAnimalInterests(codes: string[]) {
    return mutate<string[]>("/api/v1/members/me/preferences/animal-interests", {
      method: "PUT",
      headers: jsonHeaders,
      body: JSON.stringify({ codes }),
    });
  },
  updateOnboarding(input: OnboardingInput) {
    return mutate<Member>("/api/v1/members/me/onboarding", {
      method: "PUT",
      headers: jsonHeaders,
      body: JSON.stringify(input),
    });
  },
  updateProfile(input: { bio: string; showPublicPosts: boolean; showPublicComments: boolean; showPublicPets: boolean; showPublicReactions: boolean }) {
    return mutate<Member>("/api/v1/members/me/profile", {
      method: "PUT",
      headers: jsonHeaders,
      body: JSON.stringify(input),
    });
  },
};

export const catalogApi = {
  neighborhoods(signal?: AbortSignal) {
    return cachedGet<Neighborhood[]>("catalog:neighborhoods", "/api/v1/catalog/neighborhoods", signal, 5 * 60_000);
  },
  neighborhood(slug: string, signal?: AbortSignal) {
    const path = `/api/v1/catalog/neighborhoods/${encodeURIComponent(slug)}`;
    return cachedGet<Neighborhood>(`catalog:neighborhood:${slug}`, path, signal, 5 * 60_000);
  },
  breed(code: string, signal?: AbortSignal) {
    const path = `/api/v1/catalog/breeds/${encodeURIComponent(code)}`;
    return cachedGet<Breed>(`catalog:breed:${code}`, path, signal, 5 * 60_000);
  },
  animalInterests(signal?: AbortSignal) {
    return cachedGet<AnimalInterest[]>("catalog:animal-interests", "/api/v1/catalog/animal-interests", signal, 5 * 60_000);
  },
};

export const localResourceApi = {
  list(kind?: LocalResourceKind, query = "", signal?: AbortSignal) {
    const search = new URLSearchParams();
    if (kind) search.set("kind", kind);
    if (query.trim()) search.set("query", query.trim());
    return apiFetch<LocalResource[]>(`/api/v1/local-resources?${search}`, { signal });
  },
  detail(resourceId: string, signal?: AbortSignal) {
    return apiFetch<LocalResource>(`/api/v1/local-resources/${encodeURIComponent(resourceId)}`, { signal });
  },
};

export const gatheringApi = {
  list(signal?: AbortSignal) { return apiFetch<Gathering[]>("/api/v1/gatherings", { signal }); },
  detail(id: string, signal?: AbortSignal) { return apiFetch<Gathering>(`/api/v1/gatherings/${encodeURIComponent(id)}`, { signal }); },
  create(input: { title: string; description: string; location: string; startsAt: string; capacity: number }) { return mutate<Gathering>("/api/v1/gatherings", { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) }); },
  join(id: string) { return mutate<Gathering>(`/api/v1/gatherings/${encodeURIComponent(id)}/participants`, { method: "POST", headers: jsonHeaders }); },
  leave(id: string) { return mutate<Gathering>(`/api/v1/gatherings/${encodeURIComponent(id)}/participants/me`, { method: "DELETE", headers: jsonHeaders }); },
  cancel(id: string) { return mutate<Gathering>(`/api/v1/gatherings/${encodeURIComponent(id)}/cancel`, { method: "PATCH", headers: jsonHeaders }); },
};

export const adoptionApi = {
  create(input: { title: string; description: string; species: string; breed?: string; neighborhoodId?: string }) {
    return mutate<AdoptionListing>("/api/v1/adoptions", { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) });
  },
};

export const trustApi = {
  report(input: { targetType: string; targetId: string; reason: TrustReportReason; detail?: string }) { return mutate("/api/v1/trust-reports", { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) }); },
};

export const adminModerationApi = {
  reviewReport(id: string, status: "REVIEWED" | "REJECTED") { return mutate(`/api/admin/reports/${encodeURIComponent(id)}`, { method: "PATCH", headers: jsonHeaders, body: JSON.stringify({ status }) }); },
  setPublicationVisibility(id: string, visible: boolean, reason: string) { return mutate<{ id: string; lifecycle: string }>(`/api/admin/moderation/posts/${encodeURIComponent(id)}/visibility`, { method: "PATCH", headers: jsonHeaders, body: JSON.stringify({ visible, reason }) }); },
  memberAction(action: "sanction" | "hide-content" | "restore-content", memberId: string, reason: string) { return mutate<{ memberId: string; action: string; affectedPublications: number }>(`/api/admin/moderation/users/${action}`, { method: "POST", headers: jsonHeaders, body: JSON.stringify({ memberId, reason }) }); },
  mediaCleanup(dryRun: boolean) { return mutate<{ candidateCount: number; candidateBytes: number; deletedCount: number; observedAt: string }>(`/api/v1/operations/media/uploads/cleanup?dryRun=${dryRun}`, { method: "POST" }); },
};
export const adminPolicyApi = {
  get(key: string, signal?: AbortSignal) { return apiFetch<PolicyDocument>(`/api/admin/policies?key=${encodeURIComponent(key)}`, { signal }); },
  update(input: { key: string; title: string; body: string }) { return mutate<PolicyDocument>("/api/admin/policies", { method: "PUT", headers: jsonHeaders, body: JSON.stringify(input) }); },
};

export const notificationApi = {
  list(unread = false, signal?: AbortSignal) { return apiFetch<Notification[]>(`/api/v1/notifications${unread ? "?unread=true" : ""}`, { signal }); },
  unreadCount(signal?: AbortSignal) { return apiFetch<{ count: number }>("/api/v1/notifications/unread-count", { signal }); },
  markRead(id: string) { return mutate<Notification>(`/api/v1/notifications/${encodeURIComponent(id)}/read`, { method: "PATCH", headers: jsonHeaders }); },
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
    const path = `/api/v1/publications/${encodeURIComponent(publicationId)}`;
    return cachedGet<Publication>(`publication:${publicationId}`, path, signal, 30_000);
  },
  async edit(publicationId: string, input: EditPublicationInput) {
    const updated = await mutate<Publication>(`/api/v1/publications/${encodeURIComponent(publicationId)}`, {
      method: "PUT",
      headers: jsonHeaders,
      body: JSON.stringify(input),
    });
    invalidateCachedGet(`publication:${publicationId}`);
    return updated;
  },
  async delete(publicationId: string, version: number) {
    const result = await mutate<void>(`/api/v1/publications/${encodeURIComponent(publicationId)}`, {
      method: "DELETE",
      headers: jsonHeaders,
      body: JSON.stringify({ version }),
    });
    invalidateCachedGet(`publication:${publicationId}`);
    return result;
  },
  share(publicationId: string) {
    return mutate<{ path: string }>(`/api/posts/${encodeURIComponent(publicationId)}/share`, {
      method: "POST",
    });
  },
  view(publicationId: string) {
    return mutate<PublicationStats>(`/api/posts/${encodeURIComponent(publicationId)}/view`, { method: "POST" });
  },
  stats(publicationId: string, signal?: AbortSignal) {
    return apiFetch<PublicationStats>(`/api/posts/${encodeURIComponent(publicationId)}/stats`, { signal });
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
  async setBookmark(publicationId: string, active: boolean) {
    const result = await mutate<Bookmark>(
      `/api/v1/publications/${encodeURIComponent(publicationId)}/bookmark`,
      {
        method: "PUT",
        headers: jsonHeaders,
        body: JSON.stringify({ active }),
      },
    );
    invalidateCachedGet("member:bookmarks");
    return result;
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
    query,
    scope = "ALL",
    from,
    to,
    animalInterestCodes,
    type,
  }: {
    audience?: "GLOBAL" | "VIEWER";
    cursor?: string;
    limit?: number;
    signal?: AbortSignal;
    query?: string;
    scope?: "ALL" | "GLOBAL" | "LOCAL";
    from?: string;
    to?: string;
    animalInterestCodes?: string[];
    type?: string;
  } = {}) {
    const search = new URLSearchParams({ audience, limit: String(limit), scope });
    if (cursor) search.set("cursor", cursor);
    if (query) search.set("query", query);
    if (from) search.set("from", from);
    if (to) search.set("to", to);
    if (animalInterestCodes) search.set("animals", animalInterestCodes.join(","));
    if (type) search.set("type", type);
    return apiFetch<FeedPage>(`/api/v1/feed?${search}`, { signal });
  },
};

export const communityApi = {
  feed(
    animalCode: string,
    board = "all",
    options: {
      audience?: "GLOBAL" | "VIEWER";
      cursor?: string;
      limit?: number;
      signal?: AbortSignal;
      query?: string;
      scope?: "ALL" | "GLOBAL" | "LOCAL";
    } = {},
  ) {
    const search = new URLSearchParams({
      audience: options.audience ?? "VIEWER",
      board,
      limit: String(options.limit ?? 20),
      scope: options.scope ?? "ALL",
    });
    if (options.cursor) search.set("cursor", options.cursor);
    if (options.query) search.set("query", options.query);
    return apiFetch<CommunityFeedPage>(
      `/api/v1/communities/${encodeURIComponent(animalCode)}/feed?${search}`,
      { signal: options.signal },
    );
  },
};

export const commonBoardApi = {
  feed(
    board = "all",
    options: {
      audience?: "GLOBAL" | "VIEWER";
      cursor?: string;
      limit?: number;
      signal?: AbortSignal;
      query?: string;
      scope?: "ALL" | "GLOBAL" | "LOCAL";
    } = {},
  ) {
    const search = new URLSearchParams({
      audience: options.audience ?? "VIEWER",
      limit: String(options.limit ?? 20),
      scope: options.scope ?? "ALL",
    });
    if (options.cursor) search.set("cursor", options.cursor);
    if (options.query) search.set("query", options.query);
    return apiFetch<FeedPage>(
      `/api/v1/boards/${encodeURIComponent(board)}/feed?${search}`,
      { signal: options.signal },
    );
  },
};

export const mediaApi = {
  create(input: { checksumSha256: string; contentType: string; byteSize: number }) { return mutate<MediaUpload>("/api/v1/media/uploads", { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) }); },
  uploadContent(id: string, file: File) { const body = new FormData(); body.append("file", file); return mutate<MediaUpload>(`/api/v1/media/uploads/${encodeURIComponent(id)}/content`, { method: "PUT", body }); },
  finalize(id: string, checksumSha256: string) { return mutate<MediaUpload>(`/api/v1/media/uploads/${encodeURIComponent(id)}/finalize`, { method: "POST", headers: jsonHeaders, body: JSON.stringify({ checksumSha256 }) }); },
  attach(id: string, publicationId: string) { return mutate<MediaUpload>(`/api/v1/media/uploads/${encodeURIComponent(id)}/attachments/publications/${encodeURIComponent(publicationId)}`, { method: "POST", headers: jsonHeaders }); },
};

export const guestApi = {
  createAuthor(password: string) {
    return mutate<{ guestId: string }>("/api/guest/authors", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ password }),
    });
  },
  createPublication(input: { password: string; title: string; body: string }) {
    return mutate<{ id: string }>("/api/guest/posts", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify(input),
    });
  },
  createComment(publicationId: string, input: { password: string; body: string; parentCommentId?: string }) {
    return mutate<{ id: string }>(`/api/guest/posts/${encodeURIComponent(publicationId)}/comments`, {
      method: "POST", headers: jsonHeaders, body: JSON.stringify(input),
    });
  },
  updatePublication(publicationId: string, input: { password: string; title: string; body: string; version: number }) {
    return mutate<{ id: string; title: string; body: string; version: number }>(`/api/guest/posts/${encodeURIComponent(publicationId)}`, {
      method: "PATCH", headers: jsonHeaders, body: JSON.stringify(input),
    });
  },
  deletePublication(publicationId: string, input: { password: string; version: number }) {
    return mutate<void>(`/api/guest/posts/${encodeURIComponent(publicationId)}`, {
      method: "DELETE", headers: jsonHeaders, body: JSON.stringify(input),
    });
  },
};

export const careApi = {
  list(signal?: AbortSignal) { return apiFetch<CareRequest[]>("/api/v1/care/requests", { signal }); },
  detail(id: string, signal?: AbortSignal) { return apiFetch<CareRequest>(`/api/v1/care/requests/${encodeURIComponent(id)}`, { signal }); },
  applications(id: string, signal?: AbortSignal) { return apiFetch<CareApplication[]>(`/api/v1/care/requests/${encodeURIComponent(id)}/applications`, { signal }); },
  assignment(id: string, signal?: AbortSignal) { return apiFetch<CareAssignment>(`/api/v1/care/requests/${encodeURIComponent(id)}/assignment`, { signal }); },
  create(input: { title: string; description: string; location: string; startsAt: string; endsAt: string; rewardHint?: string }) { return mutate<CareRequest>("/api/v1/care/requests", { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) }); },
  cancel(id: string, version: number) { return mutate<CareRequest>(`/api/v1/care/requests/${encodeURIComponent(id)}/cancel`, { method: "PATCH", headers: jsonHeaders, body: JSON.stringify({ version }) }); },
  apply(requestId: string, message: string) { return mutate<CareApplication>(`/api/v1/care/requests/${encodeURIComponent(requestId)}/applications`, { method: "POST", headers: jsonHeaders, body: JSON.stringify({ message }) }); },
  accept(requestId: string, applicationId: string, version: number) { return mutate<CareAssignment>(`/api/v1/care/requests/${encodeURIComponent(requestId)}/applications/${encodeURIComponent(applicationId)}/accept`, { method: "POST", headers: jsonHeaders, body: JSON.stringify({ version }) }); },
  transition(assignmentId: string, status: CareAssignment["status"], version: number) { return mutate<CareAssignment>(`/api/v1/care/assignments/${encodeURIComponent(assignmentId)}/status`, { method: "PATCH", headers: jsonHeaders, body: JSON.stringify({ status, version }) }); },
  feedback(assignmentId: string, body: string) { return mutate(`/api/v1/care/assignments/${encodeURIComponent(assignmentId)}/feedback`, { method: "POST", headers: jsonHeaders, body: JSON.stringify({ body }) }); },
};
export const volunteerApi = { list(signal?: AbortSignal) { return apiFetch<VolunteerOpportunity[]>("/api/v1/volunteer", { signal }); }, apply(id: string, message: string) { return mutate<void>(`/api/v1/volunteer/${encodeURIComponent(id)}/applications`, { method: "POST", headers: jsonHeaders, body: JSON.stringify({ message }) }); }, create(input: { title: string; description: string; organization: string; location: string; startsAt: string; capacity: number }) { return mutate<VolunteerOpportunity>("/api/v1/volunteer", { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) }); } };
export const hospitalReviewApi = { list(query = "", signal?: AbortSignal) { const params = query ? `?hospital=${encodeURIComponent(query)}` : ""; return apiFetch<HospitalReview[]>(`/api/v1/hospital-reviews${params}`, { signal }); }, create(input: { hospitalName: string; address: string; rating: number; body: string }) { return mutate<HospitalReview>("/api/v1/hospital-reviews", { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) }); }, flag(id: string, input: { reason: string; detail?: string }) { return mutate(`/api/v1/hospital-reviews/${encodeURIComponent(id)}/flags`, { method: "POST", headers: jsonHeaders, body: JSON.stringify(input) }); } };

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
