package com.townpet.publication;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
class LegacyShareCardController {
  private final PublicationService publications;

  LegacyShareCardController(PublicationService publications) {
    this.publications = publications;
  }

  @GetMapping(value = {"/api/posts/{publicationId}/lost-found-share.svg", "/api/posts/{publicationId}/share.svg"}, produces = "image/svg+xml")
  ResponseEntity<byte[]> share(@PathVariable UUID publicationId) {
    PublicationEntity publication =
        publications
            .findVisible(publicationId, null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    String title = escape(publication.getTitle());
    String body = escape(publication.getBody());
    String svg =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"630\" viewBox=\"0 0 1200 630\"><rect width=\"1200\" height=\"630\" fill=\"#f3efe6\"/><text x=\"80\" y=\"150\" font-family=\"sans-serif\" font-size=\"34\" fill=\"#55705c\">TOWNPET</text><text x=\"80\" y=\"290\" font-family=\"sans-serif\" font-size=\"52\" font-weight=\"700\" fill=\"#243229\">"
            + title
            + "</text><text x=\"80\" y=\"370\" font-family=\"sans-serif\" font-size=\"28\" fill=\"#65736a\">"
            + body.substring(0, Math.min(body.length(), 80))
            + "</text></svg>";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("image/svg+xml"))
        .body(svg.getBytes(StandardCharsets.UTF_8));
  }

  private static String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
