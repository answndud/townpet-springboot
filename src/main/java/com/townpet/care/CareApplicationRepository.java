package com.townpet.care;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CareApplicationRepository extends JpaRepository<CareApplicationEntity, UUID> {
  List<CareApplicationEntity> findTop100ByRequestIdOrderByCreatedAtAscIdAsc(UUID requestId);

  @Modifying
  @Query(
      "update CareApplicationEntity a set a.status = com.townpet.care.CareApplicationStatus.DECLINED, "
          + "a.version = a.version + 1, "
          + "a.updatedAt = :updatedAt where a.requestId = :requestId "
          + "and a.id <> :acceptedId and a.status = com.townpet.care.CareApplicationStatus.PENDING")
  int declineOtherPending(
      @Param("requestId") UUID requestId,
      @Param("acceptedId") UUID acceptedId,
      @Param("updatedAt") Instant updatedAt);

  Optional<CareApplicationEntity> findByRequestIdAndApplicantMemberId(
      UUID requestId, UUID applicantMemberId);

  Optional<CareApplicationEntity> findByIdAndRequestId(UUID id, UUID requestId);
}
