package com.townpet.catalog;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/catalog/breeds")
class BreedController {
  private final BreedRepository breeds;

  BreedController(BreedRepository breeds) {
    this.breeds = breeds;
  }

  @GetMapping
  List<Response> list() {
    return breeds.findAllByOrderBySpeciesAscNameAsc().stream()
        .map(BreedController::response)
        .toList();
  }

  @GetMapping("/{code}")
  Response get(@PathVariable String code) {
    return breeds
        .findByCode(code)
        .map(BreedController::response)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static Response response(BreedEntity breed) {
    return new Response(
        breed.getCode(), breed.getSpecies(), breed.getName(), breed.getDescription());
  }

  record Response(String code, String species, String name, String description) {}
}
