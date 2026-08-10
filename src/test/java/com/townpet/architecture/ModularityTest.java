package com.townpet.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.townpet.TownPetApplication;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

  @Test
  void detectsTheAcceptedModuleMap() {
    ApplicationModules modules = ApplicationModules.of(TownPetApplication.class);

    Set<String> detected =
        modules.stream()
            .map(module -> module.getIdentifier().toString())
            .collect(Collectors.toSet());

    assertThat(detected)
        .containsExactlyInAnyOrderElementsOf(
            Set.of(
                "common",
                "identity",
                "member",
                "catalog",
                "publication",
                "engagement",
                "localguide",
                "marketplace",
                "care",
                "welfare",
                "lostfound",
                "gathering",
                "relationship",
                "trustsafety",
                "discovery",
                "notification",
                "media",
                "operations"));
  }

  @Test
  void verifiesModuleDependenciesAndCycles() {
    ApplicationModules.of(TownPetApplication.class).verify();
  }
}
