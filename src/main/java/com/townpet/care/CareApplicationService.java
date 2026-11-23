package com.townpet.care;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CareApplicationService {
  private final CareRequestRepository requests;
  private final Clock clock;
  private final CareApplicationRepository applications;

  CareApplicationService(CareRequestRepository requests, CareApplicationRepository applications,
      Clock clock) {
    this.requests = requests;
    this.applications = applications;
    this.clock = clock;
  }

  @Transactional
  CareApplicationEntity apply(UUID applicant, UUID requestId, String message) {
    CareRequestEntity request =
        requests
            .findByIdAndStatus(requestId, CareRequestStatus.OPEN)
            .orElseThrow(CareNotFoundException::new);
    if (!request.getStartsAt().isAfter(Instant.now(clock)))
      throw new CareStateConflictException("Request has expired");
    if (request.getRequesterMemberId().equals(applicant))
      throw new CareInvalidRequestException("Requester cannot apply");
    if (applications.findByRequestIdAndApplicantMemberId(requestId, applicant).isPresent())
      throw new CareStateConflictException("Already applied");
    try {
      return applications.saveAndFlush(new CareApplicationEntity(requestId, applicant, message, Instant.now(clock)));
    } catch (DataIntegrityViolationException e) {
      throw new CareStateConflictException("Already applied", e);
    }
  }

  @Transactional(readOnly = true)
  List<CareApplicationEntity> listForRequester(UUID requester, UUID requestId) {
    CareRequestEntity request =
        requests.findById(requestId).orElseThrow(CareNotFoundException::new);
    if (!request.getRequesterMemberId().equals(requester)) throw new CareAccessDeniedException();
    return applications.findTop100ByRequestIdOrderByCreatedAtAscIdAsc(requestId);
  }

  @Transactional
  CareApplicationEntity decide(
      UUID requester,
      UUID requestId,
      UUID applicationId,
      CareApplicationStatus status,
      long version) {
    CareRequestEntity request =
        requests.findById(requestId).orElseThrow(CareNotFoundException::new);
    if (!request.getRequesterMemberId().equals(requester)) throw new CareAccessDeniedException();
    if (request.getStatus() != CareRequestStatus.OPEN) {
      throw new CareStateConflictException("Request is no longer open");
    }
    if (!request.getStartsAt().isAfter(Instant.now(clock))) {
      throw new CareStateConflictException("Request has expired");
    }
    if (status != CareApplicationStatus.ACCEPTED && status != CareApplicationStatus.DECLINED) {
      throw new CareStateConflictException("Only ACCEPTED or DECLINED is allowed");
    }
    CareApplicationEntity application =
        applications
            .findByIdAndRequestId(applicationId, requestId)
            .orElseThrow(CareNotFoundException::new);
    if (application.getVersion() != version) throw new CareVersionConflictException();
    application.changeStatus(status, Instant.now(clock));
    return applications.saveAndFlush(application);
  }
}
