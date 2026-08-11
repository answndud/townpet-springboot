package com.townpet.operations;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionRequestRepository extends JpaRepository<CorrectionRequestEntity, UUID> {}
