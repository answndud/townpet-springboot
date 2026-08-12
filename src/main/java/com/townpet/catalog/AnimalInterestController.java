package com.townpet.catalog;

import com.townpet.catalog.api.AnimalInterestCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/animal-interests")
class AnimalInterestController {
  @GetMapping
  List<AnimalInterestCatalog.Option> list() {
    return AnimalInterestCatalog.options();
  }
}
