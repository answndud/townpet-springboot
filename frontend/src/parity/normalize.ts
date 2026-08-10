const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const ISO_TIMESTAMP_PATTERN = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3,9})?Z$/;
const SIGNED_QUERY_PATTERN = /(?:^|&)(?:X-Amz-[^=]+|signature|expires|token)=/i;
const IGNORED_FIELDS = new Set(["traceId", "requestId", "generatedAt"]);

export function normalizePayload(value: unknown, key?: string): unknown {
  if (key && IGNORED_FIELDS.has(key)) {
    return "<volatile>";
  }
  if (typeof value === "string") {
    if (UUID_PATTERN.test(value)) return "<uuid>";
    if (ISO_TIMESTAMP_PATTERN.test(value)) return "<timestamp>";
    if (value.includes("?") && SIGNED_QUERY_PATTERN.test(value.split("?")[1] ?? "")) {
      return "<signed-url>";
    }
    return value;
  }
  if (Array.isArray(value)) return value.map((item) => normalizePayload(item));
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([entryKey, entryValue]) => [entryKey, normalizePayload(entryValue, entryKey)]),
    );
  }
  return value;
}

export function normalizedJson(value: unknown): string {
  return JSON.stringify(normalizePayload(value));
}

export function equivalentPayloads(left: unknown, right: unknown): boolean {
  return normalizedJson(left) === normalizedJson(right);
}
