package com.townpet.care;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CareRequestService {
  private final CareRequestRepository requests;

  CareRequestService(CareRequestRepository requests) {
    this.requests = requests;
  }

  @Transactional(readOnly = true)
  List<CareRequestEntity> open() {
    return requests.findTop100ByStatusOrderByStartsAtAscIdAsc(CareRequestStatus.OPEN);
  }

  @Transactional(readOnly = true)
  Optional<CareRequestEntity> get(UUID id) {
    return requests.findById(id);
  }

  @Transactional
  CreateResult create(
      UUID memberId,
      String title,
      String description,
      String location,
      Instant startsAt,
      Instant endsAt,
      String rewardHint) {
    if (!endsAt.isAfter(startsAt))
      throw new IllegalArgumentException("endsAt must be after startsAt");
    return new CreateResult(
        requests.saveAndFlush(
            new CareRequestEntity(
                memberId, title, description, location, startsAt, endsAt, rewardHint)));
  }

  @Transactional
  void cancel(UUID memberId, UUID id, long version) {
    CareRequestEntity request =
        requests
            .findByIdAndStatus(id, CareRequestStatus.OPEN)
            .orElseThrow(NoSuchElementException::new);
    if (!request.getRequesterMemberId().equals(memberId)) throw new SecurityException();
    if (request.getVersion() != version) throw new IllegalStateException("version conflict");
    request.cancel();
    requests.saveAndFlush(request);
  }

  record CreateResult(CareRequestEntity request) {}
}
