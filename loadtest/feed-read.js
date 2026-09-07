import { optionsFor, get } from "./common.js";

export const options = optionsFor();

export default function () {
  get("/api/v1/discovery?limit=20", "public-discovery");
}
