package com.chubb.claims.workflow;

import com.chubb.claims.claim.ClaimStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "claim_history")
public class ClaimHistory {
    @Id private UUID id;
    @Column(nullable = false) private UUID claimId;
    @Column(nullable = false) private String operation;
    @Enumerated(EnumType.STRING) private ClaimStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ClaimStatus toStatus;
    @Column(nullable = false) private Instant occurredAt;
    @Column(nullable = false) private long aggregateVersion;
    protected ClaimHistory() {}
    public ClaimHistory(UUID eventId, UUID claimId, String operation, ClaimStatus from, ClaimStatus to, Instant now, long version) {
        id = eventId; this.claimId = claimId; this.operation = operation; fromStatus = from; toStatus = to; occurredAt = now; aggregateVersion = version;
    }
    public UUID getId() { return id; }
    public String getOperation() { return operation; }
    public ClaimStatus getFromStatus() { return fromStatus; }
    public ClaimStatus getToStatus() { return toStatus; }
    public Instant getOccurredAt() { return occurredAt; }
    public long getAggregateVersion() { return aggregateVersion; }
}
