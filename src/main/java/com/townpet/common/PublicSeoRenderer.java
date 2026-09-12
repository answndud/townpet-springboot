package com.townpet.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PublicSeoRenderer {
  private static final String ORIGIN = "https://townpet.cloud";
  private static final Pattern EMAIL =
      Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
  private static final Pattern PHONE =
      Pattern.compile("(?:\\+?82[- ]?)?0?1[0-9][- ]?[0-9]{3,4}[- ]?[0-9]{4}");
  private static final Pattern COORDINATE =
      Pattern.compile("[-+]?\\d{1,3}\\.\\d{3,}\\s*[,/]\\s*[-+]?\\d{1,3}\\.\\d{3,}");

  public ResponseEntity<String> page(String path, PublicSeoProvider.SeoPage page) {
    return response(HttpStatus.OK, path, page, true);
  }

  public ResponseEntity<String> notFound(String path) {
    return response(
        HttpStatus.NOT_FOUND,
        path,
        new PublicSeoProvider.SeoPage(
            "페이지를 찾을 수 없습니다",
            "요청한 공개 콘텐츠가 없거나 더 이상 공개되지 않습니다.",
            "요청한 공개 콘텐츠가 없거나 더 이상 공개되지 않습니다.",
            null,
            null),
        false);
  }

  private ResponseEntity<String> response(
      HttpStatus status, String path, PublicSeoProvider.SeoPage page, boolean indexable) {
    String canonical = ORIGIN + normalizePath(path);
    String title = "TownPet | " + page.title();
    String description = safeDescription(page.description());
    String body = paragraphs(page.body());
    String source = sourceMarkup(page);
    String robots = indexable ? "index,follow" : "noindex,follow";
    String schema = indexable ? articleSchema(page, canonical, description) : "";
    String html =
        "<!doctype html><html lang=\"ko\"><head>"
            + "<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<meta name=\"robots\" content=\""
            + escape(robots)
            + "\"><meta name=\"description\" content=\""
            + escape(description)
            + "\">"
            + "<link rel=\"canonical\" href=\""
            + escape(canonical)
            + "\"><title>"
            + escape(title)
            + "</title>"
            + "<meta property=\"og:type\" content=\"article\"><meta property=\"og:site_name\" content=\"TownPet\">"
            + "<meta property=\"og:locale\" content=\"ko_KR\"><meta property=\"og:image\" content=\""
            + ORIGIN
            + "/townpet-logo.svg\"><meta property=\"og:image:alt\" content=\"TownPet 공개 반려생활 커뮤니티\">"
            + "<meta property=\"og:title\" content=\""
            + escape(title)
            + "\"><meta property=\"og:description\" content=\""
            + escape(description)
            + "\"><meta property=\"og:url\" content=\""
            + escape(canonical)
            + "\"><meta name=\"twitter:card\" content=\"summary\"><meta name=\"twitter:title\" content=\""
            + escape(title)
            + "\"><meta name=\"twitter:description\" content=\""
            + escape(description)
            + "\"><meta name=\"twitter:image\" content=\""
            + ORIGIN
            + "/townpet-logo.svg\">"
            + (schema.isEmpty()
                ? ""
                : "<script type=\"application/ld+json\">" + schema + "</script>")
            + "</head><body><div id=\"root\"><main><article><p>TownPet 공개 콘텐츠</p><h1>"
            + escape(page.title())
            + "</h1>"
            + body
            + source
            + "</article><p><a href=\"/\">TownPet 홈으로 이동</a></p></main></div>"
            + "<script type=\"module\" src=\"/assets/index.js\"></script></body></html>";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(
        new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8));
    headers.set("X-Robots-Tag", robots);
    return new ResponseEntity<>(html, headers, status);
  }

  private static String articleSchema(
      PublicSeoProvider.SeoPage page, String canonical, String description) {
    StringBuilder json =
        new StringBuilder(
                "{\"@context\":\"https://schema.org\",\"@type\":\"Article\",\"headline\":\"")
            .append(escapeJson(page.title()))
            .append("\",\"description\":\"")
            .append(escapeJson(description))
            .append("\",\"mainEntityOfPage\":\"")
            .append(escapeJson(canonical))
            .append("\",\"inLanguage\":\"ko-KR\",\"isPartOf\":{\"@id\":\"https://townpet.cloud/#website\"}");
    appendDate(json, "datePublished", page.publishedAt());
    appendDate(json, "dateModified", page.modifiedAt());
    String sourceUrl = httpSourceUrl(page.sourceUrl());
    if (sourceUrl != null) {
      json.append(",\"citation\":\"").append(escapeJson(sourceUrl)).append("\"");
    }
    return json.append("}").toString();
  }

  private static String sourceMarkup(PublicSeoProvider.SeoPage page) {
    String sourceName = page.sourceName();
    String sourceUrl = httpSourceUrl(page.sourceUrl());
    if ((sourceName == null || sourceName.isBlank()) && sourceUrl == null && page.modifiedAt() == null) return "";
    StringBuilder markup = new StringBuilder("<footer>");
    if (sourceName != null && !sourceName.isBlank()) {
      markup.append("<p>출처: ").append(escape(sourceName)).append("</p>");
    }
    if (page.modifiedAt() != null) {
      markup.append("<p>최종 확인: ").append(escape(page.modifiedAt().toString())).append("</p>");
    }
    if (sourceUrl != null) {
      markup.append("<p><a href=\"").append(escape(sourceUrl)).append("\" rel=\"nofollow noreferrer\">원문 보기</a></p>");
    }
    return markup.append("</footer>").toString();
  }

  private static @Nullable String httpSourceUrl(@Nullable String value) {
    if (value == null || value.isBlank()) return null;
    try {
      URI uri = new URI(value.trim());
      String scheme = uri.getScheme();
      return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) ? uri.toString() : null;
    } catch (URISyntaxException exception) {
      return null;
    }
  }

  private static void appendDate(StringBuilder json, String name, @Nullable Instant value) {
    if (value != null) json.append(",\"").append(name).append("\":\"").append(value).append("\"");
  }

  private static String safeDescription(String value) {
    String cleaned = EMAIL.matcher(value == null ? "" : value).replaceAll("");
    cleaned = PHONE.matcher(cleaned).replaceAll("");
    cleaned = COORDINATE.matcher(cleaned).replaceAll("");
    cleaned = cleaned.replaceAll("\\s+", " ").trim();
    return cleaned.substring(0, Math.min(cleaned.length(), 160));
  }

  private static String paragraphs(String value) {
    if (value == null || value.isBlank()) return "<p>TownPet에서 공개 반려생활 정보를 확인하세요.</p>";
    StringBuilder result = new StringBuilder();
    for (String paragraph : value.split("\\R+", -1)) {
      if (!paragraph.isBlank())
        result.append("<p>").append(escape(paragraph.trim())).append("</p>");
    }
    return result.toString();
  }

  private static String normalizePath(String path) {
    String clean = path.split("[?#]", 2)[0].replaceAll("/+$", "");
    return clean.isEmpty() ? "/" : (clean.startsWith("/") ? clean : "/" + clean);
  }

  private static String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private static String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }
}
