import http from "k6/http";
import { check, sleep } from "k6";
import {
  BASE_URL,
  authHeaders,
  loginModerator,
  loginPerfMember,
  optionsFor,
} from "./common.js";

export const options = optionsFor();
let csrfToken = "";
let moderator = false;

function jsonHeaders() {
  const headers = authHeaders(csrfToken);
  headers["Content-Type"] = "application/json";
  return headers;
}

function get(path, endpoint) {
  const response = http.get(`${BASE_URL}${path}`, {
    headers: authHeaders(csrfToken),
    tags: { endpoint },
  });
  check(response, { [`${endpoint} status is 2xx`]: (item) => item.status >= 200 && item.status < 300 });
  return response;
}

export default function () {
  if (!csrfToken) {
    moderator = __VU % 20 === 0;
    csrfToken = moderator ? loginModerator() : loginPerfMember();
  }

  const choice = Math.random();
  if (choice < 0.55) {
    get("/api/v1/feed?audience=GLOBAL&limit=20", "mixed-public-feed");
  } else if (choice < 0.80 && !moderator) {
    get("/api/v1/members/me", "mixed-member-profile");
    get("/api/v1/notifications/unread-count", "mixed-notification-count");
  } else if (choice < 0.95 && !moderator) {
    const suffix = `${__VU}-${__ITER}-${Date.now()}`;
    const response = http.post(
      `${BASE_URL}/api/v1/publications`,
      JSON.stringify({
        title: `perf-mixed-${suffix}`,
        body: "Synthetic mixed workload publication",
        scope: "GLOBAL",
        neighborhoodId: null,
      }),
      { headers: jsonHeaders(), tags: { endpoint: "mixed-publication-create" } },
    );
    check(response, { "mixed write status is 201": (item) => item.status === 201 });
  } else if (moderator) {
    get("/api/admin/reports?status=OPEN", "mixed-moderator-queue");
    get("/api/admin/moderation-logs", "mixed-moderator-log");
  } else {
    get("/api/v1/feed/popular", "mixed-popular-feed");
  }
  sleep(0.1);
}
