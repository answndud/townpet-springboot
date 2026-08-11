package com.townpet.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreedRepository extends JpaRepository<BreedEntity, String> {
  List<BreedEntity> findAllByOrderBySpeciesAscNameAsc();

  Optional<BreedEntity> findByCode(String code);
}
