package com.townpet.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "member_animal_interest",
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "interest_code"}))
public class MemberAnimalInterestEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID memberId;

  @Column(nullable = false, length = 40)
  private String interestCode;

  @Column(nullable = false)
  private Instant createdAt;

  protected MemberAnimalInterestEntity() {}

  public MemberAnimalInterestEntity(UUID memberId, String interestCode) {
    this.id = UUID.randomUUID();
    this.memberId = memberId;
    this.interestCode = interestCode;
    this.createdAt = Instant.now();
  }

  public String getInterestCode() {
    return interestCode;
  }
}
