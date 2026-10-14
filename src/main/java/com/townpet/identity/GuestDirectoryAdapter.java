package com.townpet.identity;

import com.townpet.publication.api.GuestDirectory;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class GuestDirectoryAdapter implements GuestDirectory {
  private final GuestAuthorRepository guests;
  private final PasswordEncoder passwords;
  private final GuestStepUpService steps;

  GuestDirectoryAdapter(GuestAuthorRepository guests, PasswordEncoder passwords, GuestStepUpService steps) {
    this.guests = guests;
    this.passwords = passwords;
    this.steps = steps;
  }

  @Override
  public GuestIdentity authenticate(UUID publicId, String password) {
    GuestAuthorEntity guest =
        guests
            .findByPublicId(publicId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Guest identity is required"));
    if (!passwords.matches(password, guest.getManagementPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid guest credential");
    }
    return new GuestIdentity(guest.getId(), guest.getPublicId());
  }

  @Override
  public GuestIdentity createCommentAuthor(String password) {
    GuestAuthorEntity guest = steps.createGuest(password);
    return new GuestIdentity(guest.getId(), guest.getPublicId());
  }

  @Override
  public GuestIdentity authenticateByInternalId(UUID internalId, String password) {
    GuestAuthorEntity guest = steps.authenticateByInternalId(internalId, password);
    return new GuestIdentity(guest.getId(), guest.getPublicId());
  }
}
