package com.townpet.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_pet")
public class MemberPetEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID memberId;

  @Column(nullable = false, length = 40)
  private String name;

  @Column(nullable = false, length = 20)
  private String species;

  @Column(nullable = false)
  private Instant createdAt;

  protected MemberPetEntity() {}

  public MemberPetEntity(UUID id, UUID memberId, String name, String species) {
    this.id = id;
    this.memberId = memberId;
    this.name = name;
    this.species = species;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getMemberId() {
    return memberId;
  }

  public String getName() {
    return name;
  }

  public String getSpecies() {
    return species;
  }
}
