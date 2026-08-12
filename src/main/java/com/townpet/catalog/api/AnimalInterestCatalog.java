package com.townpet.catalog.api;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Stable catalog used by the member preference API and the public header. */
public final class AnimalInterestCatalog {
  private static final List<Option> OPTIONS =
      List.of(
          new Option("DOG", "강아지 & 고양이", "강아지", 10),
          new Option("CAT", "강아지 & 고양이", "고양이", 20),
          new Option("PARROT", "조류", "앵무새", 30),
          new Option("BIRD", "조류", "조류", 40),
          new Option("TURTLE", "파충류 & 양서류", "거북", 50),
          new Option("LIZARD", "파충류 & 양서류", "도마뱀", 60),
          new Option("SNAKE", "파충류 & 양서류", "뱀", 70),
          new Option("AMPHIBIAN", "파충류 & 양서류", "양서류", 80),
          new Option("REPTILE", "파충류 & 양서류", "파충류", 90),
          new Option("SMALL_ANIMAL", "소동물", "소동물", 100),
          new Option("AQUARIUM_FISH", "어류 / 수조", "어류·수조", 110),
          new Option("ARTHROPOD_INSECT", "기타", "절지류·곤충", 120));

  private static final Set<String> CODES =
      OPTIONS.stream().map(Option::code).collect(Collectors.toUnmodifiableSet());

  private AnimalInterestCatalog() {}

  public static List<Option> options() {
    return OPTIONS;
  }

  public static Set<String> codes() {
    return CODES;
  }

  public record Option(String code, String group, String label, int sortOrder) {}
}
