package com.townpet.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class NeighborhoodController {
  private final NeighborhoodRepository neighborhoods;

  public NeighborhoodController(NeighborhoodRepository neighborhoods) {
    this.neighborhoods = neighborhoods;
  }

  @GetMapping("/neighborhoods")
  List<NeighborhoodResponse> listNeighborhoods() {
    return neighborhoods.findAllByOrderByNameAsc().stream()
        .map(
            neighborhood ->
                new NeighborhoodResponse(
                    neighborhood.getId(), neighborhood.getSlug(), neighborhood.getName()))
        .toList();
  }

  record NeighborhoodResponse(UUID id, String slug, String name) {}
}
