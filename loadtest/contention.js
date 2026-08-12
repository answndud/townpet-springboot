import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, authHeaders, csrf, loginPerfMember, optionsFor } from "./common.js";

const CASE = __ENV.CONTENTION_CASE || "views";
const PUBLICATION_ID = __ENV.PERF_PUBLICATION_ID || "4714c8aa-9118-6008-184f-6683425eeb1c";
const OPPORTUNITY_ID = __ENV.PERF_OPPORTUNITY_ID || "c1f4c885-b7ce-4fa4-ee11-d98535ca14dd";

export const options = CASE === "capacity"
  ? {
      scenarios: {
        capacity: {
          executor: "per-vu-iterations",
          vus: 20,
          iterations: 1,
          maxDuration: "30s",
        },
      },
      summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
      // Capacity conflicts wait behind the single opportunity row lock. Keep a
      // separate threshold so expected 409s are not mistaken for a read SLA.
      thresholds: { checks: ["rate>0.99"], http_req_duration: ["p(95)<1500"] },
    }
  : optionsFor();

let csrfToken = "";

function publicHeaders() {
  return {
    "X-XSRF-TOKEN": csrfToken,
    Cookie: `XSRF-TOKEN=${csrfToken}`,
  };
}

function jsonHeaders() {
  const headers = authHeaders(csrfToken);
  headers["Content-Type"] = "application/json";
  return headers;
}

export default function () {
  if (CASE === "capacity") {
    if (!csrfToken) csrfToken = loginPerfMember();
    const response = http.post(
      `${BASE_URL}/api/v1/volunteer/${OPPORTUNITY_ID}/applications`,
      JSON.stringify({ message: `capacity-contender-${__VU}` }),
      { headers: jsonHeaders(), tags: { endpoint: "contention-volunteer-apply" } },
    );
    check(response, {
      "capacity response is created or conflict": (item) => item.status === 201 || item.status === 409,
    });
    return;
  }

  if (!csrfToken) csrfToken = csrf();
  const response = http.post(
    `${BASE_URL}/api/posts/${PUBLICATION_ID}/view`,
    null,
    { headers: publicHeaders(), tags: { endpoint: "contention-publication-view" } },
  );
  check(response, {
    "view increment status is 200": (item) => item.status === 200,
  });
  sleep(0.05);
}
