package com.townpet.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialRepository extends JpaRepository<CredentialEntity, UUID> {
  Optional<CredentialEntity> findByEmailIgnoreCase(String email);
}
