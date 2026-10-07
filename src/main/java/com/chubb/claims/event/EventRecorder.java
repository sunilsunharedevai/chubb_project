package com.chubb.claims.event;

import com.chubb.claims.claim.*;
import com.chubb.claims.workflow.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class EventRecorder {
    private final ClaimHistoryRepository history;
    private final OutboxRepository outbox;
    private final ObjectMapper json;
    public EventRecorder(ClaimHistoryRepository history, OutboxRepository outbox, ObjectMapper json) { this.history=history; this.outbox=outbox; this.json=json; }
    public void record(Claim claim, ClaimStatus from, String type, Instant now) {
        UUID eventId=UUID.randomUUID();
        history.save(new ClaimHistory(eventId,claim.getId(),type,from,claim.getStatus(),now,claim.getVersion()));
        var envelope=new Envelope(eventId,1,type,claim.getId(),claim.getVersion(),now,claim.getStatus(),claim.getMarket(),claim.getClaimType());
        try { outbox.save(new OutboxEvent(eventId,claim.getId(),type,json.writeValueAsString(envelope),now)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize lifecycle event",e); }
    }
    public record Envelope(UUID eventId,int schemaVersion,String eventType,UUID claimId,long aggregateVersion,
        Instant occurredAt,ClaimStatus status,Market market,ClaimType claimType) {}
}
