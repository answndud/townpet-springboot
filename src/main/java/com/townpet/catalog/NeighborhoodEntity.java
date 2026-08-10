package com.townpet.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "neighborhood")
public class NeighborhoodEntity {
  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String slug;

  @Column(nullable = false, length = 120)
  private String name;

  protected NeighborhoodEntity() {}

  public NeighborhoodEntity(UUID id, String slug, String name) {
    this.id = id;
    this.slug = slug;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public String getName() {
    return name;
  }
}
