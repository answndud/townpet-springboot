package com.townpet.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_profile")
public class MemberProfileEntity {
  @Id private UUID memberId;

  @Column(length = 500)
  private String bio;

  @Column(name = "neighborhood_id")
  private UUID neighborhoodId;

  @Column(nullable = false)
  private Instant updatedAt;

  protected MemberProfileEntity() {}

  public MemberProfileEntity(UUID memberId, String bio, UUID neighborhoodId) {
    this.memberId = memberId;
    this.bio = bio;
    this.neighborhoodId = neighborhoodId;
    this.updatedAt = Instant.now();
  }

  public void update(String bio, UUID neighborhoodId) {
    this.bio = bio;
    this.neighborhoodId = neighborhoodId;
    this.updatedAt = Instant.now();
  }

  public UUID getMemberId() {
    return memberId;
  }

  public String getBio() {
    return bio;
  }

  public UUID getNeighborhoodId() {
    return neighborhoodId;
  }
}
