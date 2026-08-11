package com.townpet.publication;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PublicationMetricRepository extends JpaRepository<PublicationMetricEntity, UUID> {}
