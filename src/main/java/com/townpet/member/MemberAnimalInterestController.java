package com.townpet.member;

import com.townpet.catalog.api.AnimalInterestCatalog;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/members/me/preferences/animal-interests")
@MemberOnly
class MemberAnimalInterestController {
  private final MemberAnimalInterestRepository interests;

  MemberAnimalInterestController(MemberAnimalInterestRepository interests) {
    this.interests = interests;
  }

  @GetMapping
  List<String> list(@AuthenticationPrincipal UserDetails principal) {
    UUID memberId = memberId(principal);
    return interests.findAllByMemberId(memberId).stream()
        .map(MemberAnimalInterestEntity::getInterestCode)
        .sorted()
        .toList();
  }

  @PutMapping
  @Transactional
  List<String> update(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody AnimalInterestUpdateRequest request) {
    Set<String> codes = Set.copyOf(request.codes());
    if (codes.size() != request.codes().size()
        || !AnimalInterestCatalog.codes().containsAll(codes)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid animal interest code");
    }
    UUID memberId = memberId(principal);
    interests.deleteAllByMemberId(memberId);
    interests.saveAll(
        codes.stream()
            .sorted()
            .map(code -> new MemberAnimalInterestEntity(memberId, code))
            .toList());
    return codes.stream().sorted().toList();
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  record AnimalInterestUpdateRequest(
      @NotNull @Size(max = 12) List<@NotBlank @Size(max = 40) String> codes) {
    AnimalInterestUpdateRequest {
      codes = codes == null ? List.of() : List.copyOf(codes);
    }
  }
}
