import { sleep } from "k6";
import { get, optionsFor } from "./common.js";

export const options = optionsFor();

export default function () {
  const requests = [
    ["/api/v1/feed?audience=GLOBAL&limit=20", "public-feed"],
    ["/api/v1/feed/popular", "popular-feed"],
    ["/api/v1/local-resources", "local-resources"],
  ];
  const [path, name] = requests[Math.floor(Math.random() * requests.length)];
  get(path, name);
  sleep(0.2);
}
