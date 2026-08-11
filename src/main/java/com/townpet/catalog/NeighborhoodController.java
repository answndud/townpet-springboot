package com.townpet.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/v1/catalog", "/api"})
public class NeighborhoodController {
  private final NeighborhoodRepository neighborhoods;

  public NeighborhoodController(NeighborhoodRepository neighborhoods) {
    this.neighborhoods = neighborhoods;
  }

  @GetMapping({"/neighborhoods", "/communities"})
  List<NeighborhoodResponse> listNeighborhoods() {
    return neighborhoods.findAllByOrderByNameAsc().stream()
        .map(
            neighborhood ->
                new NeighborhoodResponse(
                    neighborhood.getId(), neighborhood.getSlug(), neighborhood.getName()))
        .toList();
  }

  @GetMapping({"/neighborhoods/{slug}", "/communities/{slug}"})
  NeighborhoodResponse getNeighborhood(@PathVariable String slug) {
    return neighborhoods
        .findBySlug(slug)
        .map(
            neighborhood ->
                new NeighborhoodResponse(
                    neighborhood.getId(), neighborhood.getSlug(), neighborhood.getName()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  record NeighborhoodResponse(UUID id, String slug, String name) {}
}
