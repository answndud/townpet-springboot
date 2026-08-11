package com.townpet.localguide;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LocalResourceRepository extends JpaRepository<LocalResourceEntity, UUID> {
  @Query(
      "select r from LocalResourceEntity r where (:kind is null or r.kind = :kind) and (:query = '' or lower(r.title) like lower(concat('%', :query, '%')) or lower(r.summary) like lower(concat('%', :query, '%'))) order by r.updatedAt desc, r.id desc")
  List<LocalResourceEntity> search(LocalResourceKind kind, String query);
}
