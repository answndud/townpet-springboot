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
      String actor =
          principal.getAuthorities().stream()
              .map(org.springframework.security.core.GrantedAuthority::getAuthority)
              .filter(authority -> authority.startsWith("ROLE_"))
              .map(authority -> authority.substring("ROLE_".length()))
              .findFirst()
              .orElse("MEMBER");
      return new ViewerShell(actor, UUID.fromString(principal.getUsername()));
    } catch (IllegalArgumentException exception) {
      return new ViewerShell("GUEST", null);
    }
  }

  record ViewerShell(String actor, @Nullable UUID memberId) {}
}
