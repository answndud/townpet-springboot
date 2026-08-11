package com.townpet.care;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CareApplicationService {
  private final CareRequestRepository requests;
  private final CareApplicationRepository applications;
  CareApplicationService(CareRequestRepository requests, CareApplicationRepository applications) { this.requests = requests; this.applications = applications; }
  @Transactional
  CareApplicationEntity apply(UUID applicant, UUID requestId, String message) {
    CareRequestEntity request = requests.findByIdAndStatus(requestId, CareRequestStatus.OPEN).orElseThrow(NoSuchElementException::new);
    if (request.getRequesterMemberId().equals(applicant)) throw new IllegalArgumentException("Requester cannot apply");
    if (applications.findByRequestIdAndApplicantMemberId(requestId, applicant).isPresent()) throw new IllegalStateException("Already applied");
    try { return applications.saveAndFlush(new CareApplicationEntity(requestId, applicant, message)); }
    catch (DataIntegrityViolationException e) { throw new IllegalStateException("Already applied", e); }
  }
  @Transactional(readOnly = true) List<CareApplicationEntity> listForRequester(UUID requester, UUID requestId) {
    CareRequestEntity request = requests.findById(requestId).orElseThrow(NoSuchElementException::new);
    if (!request.getRequesterMemberId().equals(requester)) throw new SecurityException();
    return applications.findByRequestIdOrderByCreatedAtAscIdAsc(requestId);
  }
  @Transactional
  CareApplicationEntity decide(UUID requester, UUID requestId, UUID applicationId, CareApplicationStatus status, long version) {
    CareRequestEntity request = requests.findById(requestId).orElseThrow(NoSuchElementException::new);
    if (!request.getRequesterMemberId().equals(requester)) throw new SecurityException();
    CareApplicationEntity application = applications.findByIdAndRequestId(applicationId, requestId).orElseThrow(NoSuchElementException::new);
    if (application.getVersion() != version) throw new IllegalStateException("version conflict");
    application.changeStatus(status); return applications.saveAndFlush(application);
  }
}
