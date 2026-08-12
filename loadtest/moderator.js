import http from "k6/http";
import { check, sleep } from "k6";
import {
  BASE_URL,
  authHeaders,
  loginModerator,
  optionsFor,
} from "./common.js";

export const options = optionsFor();
let csrfToken = "";

function adminGet(path, endpoint) {
  const response = http.get(`${BASE_URL}${path}`, {
    headers: authHeaders(csrfToken),
    tags: { endpoint },
  });
  check(response, { [`${endpoint} status is 2xx`]: (item) => item.status >= 200 && item.status < 300 });
  return response;
}

function jsonHeaders() {
  const headers = authHeaders(csrfToken);
  headers["Content-Type"] = "application/json";
  return headers;
}

function bulkReview(ids) {
  const response = http.patch(
    `${BASE_URL}/api/reports/bulk`,
    JSON.stringify({ ids, status: "REVIEWED" }),
    {
      headers: jsonHeaders(),
      tags: { endpoint: "moderator-bulk-review" },
    },
  );
  check(response, { "moderator bulk review status is 200": (item) => item.status === 200 });
}

export default function () {
  if (!csrfToken) csrfToken = loginModerator();
  const reports = adminGet("/api/admin/reports?status=OPEN", "moderator-report-queue");
  if (reports.status === 200 && Math.random() < 0.2) {
    const items = reports.json("$") || [];
    const ids = items.slice(0, 10).map((item) => item.id);
    if (ids.length > 0) bulkReview(ids);
  }
  adminGet("/api/admin/moderation-logs", "moderator-action-log");
  adminGet("/api/admin/auth-audits", "moderator-auth-audit");
  adminGet("/api/admin/moderation/direct", "moderator-case-queue");
  sleep(0.2);
}
