import http from "k6/http";
import { check } from "k6";

export const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8081";
export const MEMBER_EMAIL = __ENV.MEMBER_EMAIL || "demo-member-1@townpet.local";
export const MEMBER_PASSWORD = __ENV.MEMBER_PASSWORD || "townpet-demo-123!";
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
};

export const optionsFor = () => ({
  stages: profiles[__ENV.LOAD_PROFILE || "baseline"],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    checks: ["rate>0.99"],
    http_req_duration: ["p(95)<1000"],
  },
});

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

export function login() {
  const token = csrf();
  const response = http.post(
    `${BASE_URL}/api/v1/auth/sessions`,
    JSON.stringify({ email: MEMBER_EMAIL, password: MEMBER_PASSWORD }),
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

export function authHeaders(csrfToken) {
  return {
    "X-XSRF-TOKEN": csrfToken,
    Cookie: `SESSION=${sessionCookie}`,
  };
}
