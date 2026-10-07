package com.chubb.claims.event;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID claimId;

  @Column(nullable = false)
  private String eventType;

  @Column(nullable = false, length = 8000)
  private String payload;

  @Column(nullable = false)
  private Instant occurredAt;

  private Instant publishedAt;

  @Column(nullable = false)
  private int attempts;

  protected OutboxEvent() {}

  public OutboxEvent(UUID id, UUID claimId, String type, String payload, Instant now) {
    this.id = id;
    this.claimId = claimId;
    eventType = type;
    this.payload = payload;
    occurredAt = now;
  }

  public void published(Instant now) {
    attempts++;
    publishedAt = now;
  }

  public void failed() {
    attempts++;
  }

  public UUID getId() {
    return id;
  }

  public UUID getClaimId() {
    return claimId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public int getAttempts() {
    return attempts;
  }
}
