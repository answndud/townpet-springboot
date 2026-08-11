package com.townpet.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "breed")
public class BreedEntity {
  @Id
  @Column(length = 40)
  private String code;

  @Column(nullable = false, length = 20)
  private String species;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, length = 500)
  private String description;

  protected BreedEntity() {}

  public String getCode() {
    return code;
  }

  public String getSpecies() {
    return species;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
