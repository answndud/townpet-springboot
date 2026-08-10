export type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  traceId?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
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

/** Transport seam for the generated OpenAPI client; feature code never builds URLs directly. */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method?.toUpperCase() ?? "GET";
  const csrf = document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
  const response = await fetch(path, {
    credentials: "include",
    headers: {
      accept: "application/json",
      ...(method !== "GET" && method !== "HEAD" && csrf ? { "X-XSRF-TOKEN": decodeURIComponent(csrf) } : {}),
      ...init?.headers,
    },
    ...init,
  });

  if (!response.ok) {
    const problem = (await response.json()) as ProblemDetail;
    throw new ApiError(response.status, problem);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export async function getCsrfToken(): Promise<string> {
  const response = await apiFetch<{ token: string }>("/api/v1/auth/csrf");
  return response.token;
}
