import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, authHeaders, loginPerfMember, optionsFor } from "./common.js";

export const options = optionsFor();
let csrfToken = "";

function jsonHeaders() {
  const headers = authHeaders(csrfToken);
  headers["Content-Type"] = "application/json";
  return headers;
}

function request(method, path, body, endpoint, expectedStatus) {
  const response = http.request(
    method,
    `${BASE_URL}${path}`,
    body === null ? null : JSON.stringify(body),
    { headers: jsonHeaders(), tags: { endpoint } },
  );
  check(response, {
    [`${endpoint} status is ${expectedStatus}`]: (item) => item.status === expectedStatus,
  });
  return response;
}

export default function () {
  if (!csrfToken) csrfToken = loginPerfMember();

  const suffix = `${__VU}-${__ITER}-${Date.now()}`;
  const publication = request(
    "POST",
    "/api/v1/publications",
    {
      title: `perf-write-${suffix}`,
      body: "Synthetic write-burst publication",
      scope: "GLOBAL",
      neighborhoodId: null,
    },
    "write-publication-create",
    201,
  );
  if (publication.status !== 201) return;
  const publicationId = publication.json("id");

  request(
    "POST",
    `/api/v1/publications/${publicationId}/comments`,
    { body: `perf-write-comment-${suffix}`, parentCommentId: null },
    "write-comment-create",
    201,
  );
  request(
    "PUT",
    `/api/v1/publications/${publicationId}/reaction`,
    { active: true },
    "write-reaction-on",
    200,
  );
  request(
    "PUT",
    `/api/v1/publications/${publicationId}/reaction`,
    { active: false },
    "write-reaction-off",
    200,
  );
  request(
    "PUT",
    `/api/v1/publications/${publicationId}/bookmark`,
    { active: true },
    "write-bookmark-on",
    200,
  );
  request(
    "PUT",
    `/api/v1/publications/${publicationId}/bookmark`,
    { active: false },
    "write-bookmark-off",
    200,
  );
  const detail = http.get(`${BASE_URL}/api/v1/publications/${publicationId}`, {
    tags: { endpoint: "write-publication-readback" },
  });
  check(detail, {
    "write readback status is 200": (item) => item.status === 200,
    "write readback has title": (item) => item.body.includes(`perf-write-${suffix}`),
  });
  sleep(0.1);
}
