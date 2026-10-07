package com.chubb.claims.workflow;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClaimHistoryRepository extends JpaRepository<ClaimHistory, UUID> {
    List<ClaimHistory> findByClaimIdOrderByAggregateVersionAsc(UUID claimId);
}
