import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, authHeaders, login, optionsFor } from "./common.js";

export const options = optionsFor();
let sessionReady = false;
let csrfToken = "";

function ensureSession() {
  if (!sessionReady) {
    csrfToken = login();
    sessionReady = true;
  }
}

export default function () {
  ensureSession();
  const requests = [
    ["/api/v1/members/me", "member-me"],
    ["/api/v1/notifications", "notifications"],
    ["/api/v1/notifications/unread-count", "notification-count"],
    ["/api/v1/members/me/bookmarks", "bookmarks"],
    ["/api/v1/feed?audience=VIEWER&limit=20", "member-feed"],
  ];
  const [path, name] = requests[Math.floor(Math.random() * requests.length)];
  const response = http.get(`${BASE_URL}${path}`, {
    headers: authHeaders(csrfToken),
    tags: { endpoint: name },
  });
  check(response, {
    [`${name} status is 2xx`]: (item) => item.status >= 200 && item.status < 300,
  });
  sleep(0.2);
}
