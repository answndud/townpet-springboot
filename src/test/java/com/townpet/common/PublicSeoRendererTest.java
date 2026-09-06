package com.townpet.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PublicSeoRendererTest {
  private final PublicSeoRenderer renderer = new PublicSeoRenderer();

  @Test
  void rendersIndexablePublicHtmlWithEscapedContentAndSchema() {
    var response =
        renderer.page(
            "/posts/0198f342-13d7-7000-8000-000000000005?utm_source=search",
            new PublicSeoProvider.SeoPage(
                "A <title>",
                "연락처 test@example.com 010-1234-5678",
                "본문 <script>alert(1)</script>",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst("X-Robots-Tag")).isEqualTo("index,follow");
    assertThat(response.getBody())
        .contains("<h1>A &lt;title&gt;</h1>")
        .contains("본문 &lt;script&gt;alert(1)&lt;/script&gt;")
        .contains("https://townpet.cloud/posts/0198f342-13d7-7000-8000-000000000005")
        .doesNotContain("test@example.com")
        .doesNotContain("010-1234-5678");
  }

  @Test
  void renders404NoindexHtmlForUnavailableContent() {
    var response = renderer.notFound("/posts/missing");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders().getFirst("X-Robots-Tag")).isEqualTo("noindex,follow");
    assertThat(response.getBody())
        .contains("<meta name=\"robots\" content=\"noindex,follow\">")
        .doesNotContain("application/ld+json");
  }
}
