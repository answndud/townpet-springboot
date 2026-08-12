package com.townpet.operations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/policies")
@PreAuthorize("hasRole('MODERATOR')")
class PolicyDocumentController {
  private final JdbcTemplate jdbc;
  PolicyDocumentController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @GetMapping
  Response get(@RequestParam(defaultValue = "TERMS") String key) {
    return jdbc.query("SELECT policy_key,title,body,updated_at FROM policy_document WHERE policy_key = ?", (rs, row) -> new Response(rs.getString("policy_key"), rs.getString("title"), rs.getString("body"), rs.getTimestamp("updated_at").toInstant()), key)
        .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PutMapping
  Response update(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody UpdateRequest request) {
    int count = jdbc.update("UPDATE policy_document SET title = ?, body = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ? WHERE policy_key = ?", request.title(), request.body(), memberId(principal), request.key());
    if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return get(request.key());
  }

  private static UUID memberId(UserDetails principal) { try { return UUID.fromString(principal.getUsername()); } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED); } }
  record UpdateRequest(@NotBlank @Size(max = 40) String key, @NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 20000) String body) {}
  record Response(String key, String title, String body, Instant updatedAt) {}
}
