const SITE_ORIGIN = "https://townpet.cloud";
const DYNAMIC_SCHEMA_ID = "townpet-dynamic-schema";

export type DynamicSeoInput = {
  title: string;
  description: string;
  canonicalPath: string;
  indexable?: boolean;
  type?: "website" | "article";
  datePublished?: string;
  dateModified?: string;
};

function setMeta(attribute: "name" | "property", key: string, content: string) {
  let element = document.head.querySelector<HTMLMetaElement>(`meta[${attribute}="${key}"]`);
  if (!element) {
    element = document.createElement("meta");
    element.setAttribute(attribute, key);
    document.head.appendChild(element);
  }
  element.content = content;
}

function normalizedCanonical(path: string) {
  const [pathname] = path.split(/[?#]/, 1);
  const cleanPath = pathname.replace(/\/+$/, "") || "/";
  return `${SITE_ORIGIN}${cleanPath.startsWith("/") ? cleanPath : `/${cleanPath}`}`;
}

function removeDynamicSchema() {
  document.getElementById(DYNAMIC_SCHEMA_ID)?.remove();
}

export function setDynamicSeo(input: DynamicSeoInput) {
  const title = input.title.startsWith("TownPet |") ? input.title : `TownPet | ${input.title}`;
  const description = input.description.trim().slice(0, 160);
  const canonical = normalizedCanonical(input.canonicalPath);
  const indexable = input.indexable !== false;
  const type = input.type ?? "website";

  document.title = title;
  setMeta("name", "description", description);
  setMeta("name", "robots", indexable ? "index,follow" : "noindex,follow");
  setMeta("property", "og:title", title);
  setMeta("property", "og:description", description);
  setMeta("property", "og:url", canonical);
  setMeta("property", "og:type", type);
  setMeta("name", "twitter:title", title);
  setMeta("name", "twitter:description", description);
  const canonicalLink = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (canonicalLink) canonicalLink.href = canonical;

  removeDynamicSchema();
  if (!indexable || type !== "article") return;

  const schema: Record<string, unknown> = {
    "@context": "https://schema.org",
    "@type": "Article",
    headline: input.title,
    description,
    mainEntityOfPage: canonical,
  };
  if (input.datePublished) schema.datePublished = input.datePublished;
  if (input.dateModified) schema.dateModified = input.dateModified;
  const script = document.createElement("script");
  script.id = DYNAMIC_SCHEMA_ID;
  script.type = "application/ld+json";
  script.textContent = JSON.stringify(schema);
  document.head.appendChild(script);
}
