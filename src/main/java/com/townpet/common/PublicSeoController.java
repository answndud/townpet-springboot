package com.townpet.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicSeoController {
  private final List<PublicSeoProvider> providers;
  private final PublicSeoRenderer renderer;

  public PublicSeoController(List<PublicSeoProvider> providers, PublicSeoRenderer renderer) {
    this.providers = providers;
    this.renderer = renderer;
  }

  @GetMapping({
    "/posts/{id}",
    "/adoptions/{id}",
    "/marketplace/{id}",
    "/marketplace/{id}/",
    "/lost-found/{id}",
    "/lost-found/{id}/",
    "/gatherings/{id}",
    "/gatherings/{id}/",
    "/care/{id}",
    "/care/{id}/",
    "/guides/{id}",
    "/guides/{id}/",
    "/posts/{id}/",
    "/adoptions/{id}/"
  })
  ResponseEntity<String> get(@PathVariable String id, HttpServletRequest request) {
    UUID parsed;
    try {
      parsed = UUID.fromString(id);
    } catch (IllegalArgumentException exception) {
      return renderer.notFound(request.getRequestURI());
    }
    String requestPath = request.getRequestURI();
    int routeEnd = requestPath.indexOf('/', 1);
    String route = requestPath.substring(1, routeEnd < 0 ? requestPath.length() : routeEnd);
    return providers.stream()
        .filter(provider -> provider.route().equals(route))
        .findFirst()
        .flatMap(provider -> provider.find(parsed))
        .map(page -> renderer.page(request.getRequestURI(), page))
        .orElseGet(() -> renderer.notFound(request.getRequestURI()));
  }
}
