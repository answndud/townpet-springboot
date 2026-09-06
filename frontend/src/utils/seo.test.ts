import { describe, expect, it } from "vitest";
import { setDynamicSeo } from "./seo";

describe("dynamic SEO metadata", () => {
  it("publishes a canonical Article without private fields", () => {
    document.head.innerHTML = '<link rel="canonical" href="https://townpet.cloud/" />';

    setDynamicSeo({
      title: "산책 친구를 찾았어요",
      description: "공개 게시글 설명입니다.",
      canonicalPath: "/posts/post-1/?utm_source=search",
      type: "article",
      datePublished: "2026-09-01T00:00:00Z",
    });

    expect(document.title).toBe("TownPet | 산책 친구를 찾았어요");
    expect(document.querySelector('meta[name="robots"]')?.getAttribute("content")).toBe("index,follow");
    expect(document.querySelector('link[rel="canonical"]')?.getAttribute("href")).toBe("https://townpet.cloud/posts/post-1");
    const schema = JSON.parse(document.getElementById("townpet-dynamic-schema")?.textContent ?? "{}");
    expect(schema["@type"]).toBe("Article");
    expect(schema.mainEntityOfPage).toBe("https://townpet.cloud/posts/post-1");
    expect(schema).not.toHaveProperty("author");
    expect(schema).not.toHaveProperty("location");
  });

  it("removes structured data and excludes failed detail pages", () => {
    setDynamicSeo({ title: "페이지를 찾을 수 없습니다", description: "삭제된 게시글입니다.", canonicalPath: "/posts/missing", indexable: false });

    expect(document.querySelector('meta[name="robots"]')?.getAttribute("content")).toBe("noindex,follow");
    expect(document.getElementById("townpet-dynamic-schema")).toBeNull();
  });
});
