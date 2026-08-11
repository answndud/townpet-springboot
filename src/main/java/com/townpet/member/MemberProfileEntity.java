package com.townpet.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "member_profile")
public class MemberProfileEntity {
  @Id private UUID memberId;

  @Nullable
  @Column(length = 500)
  private String bio;

  @Nullable
  @Column(name = "neighborhood_id")
  private UUID neighborhoodId;

  @Column(name = "show_public_posts", nullable = false)
  private boolean showPublicPosts = true;

  @Column(name = "show_public_comments", nullable = false)
  private boolean showPublicComments = true;

  @Column(name = "show_public_pets", nullable = false)
  private boolean showPublicPets = true;

  @Column(nullable = false)
  private Instant updatedAt;

  protected MemberProfileEntity() {}

  public MemberProfileEntity(UUID memberId, @Nullable String bio, @Nullable UUID neighborhoodId) {
    this.memberId = memberId;
    this.bio = bio;
    this.neighborhoodId = neighborhoodId;
    this.updatedAt = Instant.now();
  }

  public void update(@Nullable String bio, @Nullable UUID neighborhoodId) {
    this.bio = bio;
    this.neighborhoodId = neighborhoodId;
    this.updatedAt = Instant.now();
  }

  public void updateVisibility(boolean showPublicPosts, boolean showPublicComments, boolean showPublicPets) {
    this.showPublicPosts = showPublicPosts;
    this.showPublicComments = showPublicComments;
    this.showPublicPets = showPublicPets;
    this.updatedAt = Instant.now();
  }

  public UUID getMemberId() {
    return memberId;
  }

  public @Nullable String getBio() {
    return bio;
  }

  public @Nullable UUID getNeighborhoodId() {
    return neighborhoodId;
  }

  public boolean isShowPublicPosts() {
    return showPublicPosts;
  }

  public boolean isShowPublicComments() {
    return showPublicComments;
  }

  public boolean isShowPublicPets() {
    return showPublicPets;
  }
}
