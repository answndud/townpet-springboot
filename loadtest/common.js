import http from "k6/http";
import { check } from "k6";

export const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8081";
export const MEMBER_EMAIL = __ENV.MEMBER_EMAIL || "demo-member-1@townpet.local";
export const MEMBER_PASSWORD = __ENV.MEMBER_PASSWORD || "townpet-demo-123!";
export const MODERATOR_EMAIL = __ENV.MODERATOR_EMAIL || "demo-moderator@townpet.local";
export const MODERATOR_PASSWORD = __ENV.MODERATOR_PASSWORD || "townpet-moderator-123!";
export const PERF_MEMBER_COUNT = Number(__ENV.PERF_MEMBER_COUNT || 100);
let sessionCookie = "";

export const profiles = {
  smoke: [{ duration: "30s", target: 1 }],
  baseline: [
    { duration: "15s", target: 1 },
    { duration: "2m", target: 1 },
  ],
  calibration: [
    { duration: "15s", target: 1 },
    { duration: "3m", target: 5 },
  ],
  ramp: [
    { duration: "15s", target: 1 },
    { duration: "5m", target: 10 },
    { duration: "5m", target: 20 },
    { duration: "5m", target: 40 },
  ],
  soak: [
    { duration: "30s", target: 1 },
    { duration: "30m", target: 5 },
  ],
  spike: [
    { duration: "15s", target: 1 },
    { duration: "30s", target: 1 },
    { duration: "30s", target: 20 },
    { duration: "1m", target: 20 },
    { duration: "30s", target: 1 },
  ],
  contention: [
    { duration: "10s", target: 8 },
    { duration: "20s", target: 8 },
  ],
};

export const optionsFor = () => {
  const thresholds = {
    checks: ["rate>0.99"],
    http_req_duration: ["p(95)<1000"],
  };
  if (!expectedConflicts()) thresholds.http_req_failed = ["rate<0.01"];
  return {
    stages: profiles[__ENV.LOAD_PROFILE || "baseline"],
    summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
    thresholds,
  };
};

function expectedConflicts() {
  return __ENV.ALLOW_EXPECTED_CONFLICTS === "true";
}

export function get(path, name) {
  const response = http.get(`${BASE_URL}${path}`, {
    tags: { endpoint: name },
  });
  check(response, {
    [`${name} status is 2xx`]: (item) => item.status >= 200 && item.status < 300,
  });
  if (response.status === 0) {
    throw new Error(`${name} request failed: ${response.error || "connection error"}`);
  }
  return response;
}

export function csrf() {
  const response = http.get(`${BASE_URL}/api/v1/auth/csrf`, {
    tags: { endpoint: "auth-csrf" },
  });
  check(response, { "csrf status is 200": (item) => item.status === 200 });
  if (response.status !== 200) {
    throw new Error(`csrf request failed: HTTP ${response.status}`);
  }
  return response.json("token");
}

export function loginAs(email, password) {
  const token = csrf();
  const response = http.post(
    `${BASE_URL}/api/v1/auth/sessions`,
    JSON.stringify({ email, password }),
    {
      headers: {
        "Content-Type": "application/json",
        "X-XSRF-TOKEN": token,
      },
      tags: { endpoint: "auth-login" },
    },
  );
  check(response, { "login status is 201": (item) => item.status === 201 });
  if (response.status !== 201) {
    throw new Error(`login request failed: HTTP ${response.status}`);
  }
  sessionCookie =
    response.cookies.SESSION && response.cookies.SESSION.length > 0
      ? response.cookies.SESSION[0].value
      : "";
  if (!sessionCookie) {
    throw new Error("login response did not set a SESSION cookie");
  }
  return token;
}

export function login() {
  return loginAs(MEMBER_EMAIL, MEMBER_PASSWORD);
}

export function loginModerator() {
  return loginAs(MODERATOR_EMAIL, MODERATOR_PASSWORD);
}

export function loginPerfMember() {
  const memberNumber = ((__VU - 1) % PERF_MEMBER_COUNT) + 1;
  return loginAs(
    `perf-member-${memberNumber}@perf.townpet.local`,
    MEMBER_PASSWORD,
  );
}

export function authHeaders(csrfToken) {
  return {
    "X-XSRF-TOKEN": csrfToken,
    Cookie: `SESSION=${sessionCookie}; XSRF-TOKEN=${csrfToken}`,
  };
}
