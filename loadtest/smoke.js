import http from "k6/http";
import { check } from "k6";
import { BASE_URL, get, login, optionsFor } from "./common.js";

export const options = optionsFor();

export default function () {
  get("/actuator/health", "health");
  get("/api/v1/feed?audience=GLOBAL&limit=20", "public-feed");

  const token = login();
  const shell = http.get(`${BASE_URL}/api/viewer-shell`, {
    headers: { "X-XSRF-TOKEN": token },
    tags: { endpoint: "viewer-shell" },
  });
  check(shell, {
    "member shell status is 200": (response) => response.status === 200,
    "member shell identifies member": (response) => response.body.includes("MEMBER"),
  });
}
