package com.townpet.identity;

import java.util.UUID;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Service;

@Service
class SessionRevocationService {
  private final JdbcIndexedSessionRepository sessions;

  SessionRevocationService(JdbcIndexedSessionRepository sessions) {
    this.sessions = sessions;
  }

  void revokeAll(UUID memberId) {
    sessions.findByPrincipalName(memberId.toString()).keySet().forEach(sessions::deleteById);
  }
}
