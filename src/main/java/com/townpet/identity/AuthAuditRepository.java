package com.townpet.identity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditRepository extends JpaRepository<AuthAuditEntity, UUID> {}
