package com.townpet.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NeighborhoodRepository extends JpaRepository<NeighborhoodEntity, UUID> {
  List<NeighborhoodEntity> findAllByOrderByNameAsc();

  Optional<NeighborhoodEntity> findBySlug(String slug);
}
