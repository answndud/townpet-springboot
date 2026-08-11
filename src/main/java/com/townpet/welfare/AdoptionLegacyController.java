package com.townpet.welfare;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards/adoption/posts")
class AdoptionLegacyController {
  private final AdoptionService adoptions;

  AdoptionLegacyController(AdoptionService adoptions) {
    this.adoptions = adoptions;
  }

  @GetMapping
  List<AdoptionController.Response> list(@RequestParam(defaultValue = "20") int limit) {
    return adoptions.list(Math.min(Math.max(limit, 1), 50)).stream()
        .map(
            item ->
                new AdoptionController.Response(
                    item.id(),
                    item.publisherMemberId(),
                    item.neighborhoodId(),
                    item.title(),
                    item.description(),
                    item.species(),
                    item.breed(),
                    item.status(),
                    item.createdAt(),
                    item.updatedAt(),
                    item.version()))
        .toList();
  }
}
