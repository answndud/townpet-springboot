import { optionsFor, get } from "./common.js";

export const options = optionsFor();

export default function () {
  get("/api/v1/feed?audience=GLOBAL&limit=20", "public-feed");
}
