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
  const response = await fetch(path, {
    credentials: "include",
    headers: { accept: "application/json", ...init?.headers },
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
