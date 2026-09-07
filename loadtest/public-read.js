import { sleep } from "k6";
import { get, optionsFor } from "./common.js";

export const options = optionsFor();

export default function () {
  const requests = [
    ["/api/v1/discovery?limit=20", "public-discovery"],
    ["/api/v1/discovery/popular?limit=20", "popular-discovery"],
    ["/api/v1/local-resources", "local-resources"],
  ];
  const [path, name] = requests[Math.floor(Math.random() * requests.length)];
  get(path, name);
  sleep(0.2);
}
