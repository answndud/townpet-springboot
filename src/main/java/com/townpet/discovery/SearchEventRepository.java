package com.townpet.discovery;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SearchEventRepository extends JpaRepository<SearchEventEntity, UUID> {}
