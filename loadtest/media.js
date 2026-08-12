import http from "k6/http";
import { check, sleep } from "k6";
import crypto from "k6/crypto";
import {
  BASE_URL,
  authHeaders,
  loginPerfMember,
  optionsFor,
} from "./common.js";

export const options = optionsFor();
const PDF = "%PDF-1.4\nTownPet performance fixture\n%%EOF\n";
const CHECKSUM = crypto.sha256(PDF, "hex");
let csrfToken = "";

function jsonHeaders() {
  const headers = authHeaders(csrfToken);
  headers["Content-Type"] = "application/json";
  return headers;
}

function jsonRequest(method, path, payload, endpoint, expectedStatus) {
  const response = http.request(method, `${BASE_URL}${path}`, JSON.stringify(payload), {
    headers: jsonHeaders(),
    tags: { endpoint },
  });
  check(response, { [`${endpoint} status is ${expectedStatus}`]: (item) => item.status === expectedStatus });
  return response;
}

export default function () {
  if (!csrfToken) csrfToken = loginPerfMember();
  const suffix = `${__VU}-${__ITER}-${Date.now()}`;
  const publication = jsonRequest(
    "POST",
    "/api/v1/publications",
    {
      title: `perf-publication-media-${suffix}`,
      body: "Synthetic media publication",
      scope: "GLOBAL",
      neighborhoodId: null,
    },
    "media-publication-create",
    201,
  );
  if (publication.status !== 201) return;
  const publicationId = publication.json("id");
  const metadata = jsonRequest(
    "POST",
    "/api/v1/media/uploads",
    { checksumSha256: CHECKSUM, contentType: "application/pdf", byteSize: PDF.length },
    "media-metadata",
    200,
  );
  if (metadata.status !== 200) return;
  const assetId = metadata.json("id");
  const upload = http.put(
    `${BASE_URL}/api/v1/media/uploads/${assetId}/content`,
    { file: http.file(PDF, `perf-${suffix}.pdf`, "application/pdf") },
    { headers: authHeaders(csrfToken), tags: { endpoint: "media-content" } },
  );
  check(upload, { "media content status is 200": (item) => item.status === 200 });
  if (upload.status !== 200) return;
  const finalized = jsonRequest(
    "POST",
    `/api/v1/media/uploads/${assetId}/finalize`,
    { checksumSha256: CHECKSUM },
    "media-finalize",
    200,
  );
  if (finalized.status !== 200) return;
  const attached = http.post(
    `${BASE_URL}/api/v1/media/uploads/${assetId}/attachments/publications/${publicationId}`,
    null,
    { headers: authHeaders(csrfToken), tags: { endpoint: "media-attach" } },
  );
  check(attached, { "media attach status is 200": (item) => item.status === 200 });
  sleep(0.1);
}
