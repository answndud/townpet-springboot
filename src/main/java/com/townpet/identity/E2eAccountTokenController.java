package com.townpet.identity;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("e2e")
@RequestMapping("/api/_test/account-tokens")
final class E2eAccountTokenController {
  private final LocalAccountTokenCapture tokens;

  E2eAccountTokenController(LocalAccountTokenCapture tokens) {
    this.tokens = tokens;
  }

  @GetMapping
  ResponseEntity<TokenResponse> find(
      @RequestParam AccountTokenPurpose purpose, @RequestParam String recipient) {
    String token =
        tokens
            .find(purpose, recipient)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token not captured"));
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new TokenResponse(token));
  }

  record TokenResponse(String token) {}
}
