package com.townpet.catalog;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/breeds")
@PreAuthorize("hasRole('MODERATOR')")
class AdminBreedController {
  private final BreedRepository breeds;

  AdminBreedController(BreedRepository breeds) {
    this.breeds = breeds;
  }

  @GetMapping
  List<BreedController.Response> list() {
    return breeds.findAllByOrderBySpeciesAscNameAsc().stream()
        .map(
            item ->
                new BreedController.Response(
                    item.getCode(), item.getSpecies(), item.getName(), item.getDescription()))
        .toList();
  }
}
