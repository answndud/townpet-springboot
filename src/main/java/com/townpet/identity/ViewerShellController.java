package com.townpet.identity;

import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/viewer-shell")
class ViewerShellController {
  @GetMapping
  ViewerShell get(@AuthenticationPrincipal @Nullable UserDetails principal) {
    if (principal == null) return new ViewerShell("GUEST", null);
    try {
      return new ViewerShell("MEMBER", UUID.fromString(principal.getUsername()));
    } catch (IllegalArgumentException exception) {
      return new ViewerShell("GUEST", null);
    }
  }

  record ViewerShell(String actor, @Nullable UUID memberId) {}
}
