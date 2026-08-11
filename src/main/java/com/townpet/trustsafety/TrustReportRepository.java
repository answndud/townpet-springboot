package com.townpet.trustsafety;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
interface TrustReportRepository extends JpaRepository<TrustReportEntity,UUID>{ Optional<TrustReportEntity> findByReporterMemberIdAndTargetTypeAndTargetId(UUID reporter,String type,UUID target); List<TrustReportEntity> findByStatusOrderByCreatedAtAsc(String status); }
