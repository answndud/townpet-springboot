package com.townpet.care;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface CareApplicationRepository extends JpaRepository<CareApplicationEntity, UUID> {
  List<CareApplicationEntity> findByRequestIdOrderByCreatedAtAscIdAsc(UUID requestId);

  Optional<CareApplicationEntity> findByRequestIdAndApplicantMemberId(
      UUID requestId, UUID applicantMemberId);

  Optional<CareApplicationEntity> findByIdAndRequestId(UUID id, UUID requestId);
}
