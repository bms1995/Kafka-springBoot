package com.example.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id private String eventId;

  private String aggregateId;

  private String topic;

  private String eventType;

  @Column(columnDefinition = "text")
  private String payload;

  @Enumerated(EnumType.STRING)
  private OutboxStatus status;

  private Instant createdAt;

  private Instant publishedAt;

  private Instant nextAttemptAt;

  private Instant processingStartedAt;

  private int attemptCount;

  @Column(columnDefinition = "text")
  private String lastError;

  public OutboxEvent(String aggregateId, String topic, String eventType, String payload) {
    this.eventId = UUID.randomUUID().toString();
    this.aggregateId = aggregateId;
    this.topic = topic;
    this.eventType = eventType;
    this.payload = payload;
    this.status = OutboxStatus.PENDING;
    this.createdAt = Instant.now();
    this.nextAttemptAt = this.createdAt;
  }

  @SuppressWarnings("PMD.NullAssignment")
  public void markPublished() {
    this.status = OutboxStatus.PUBLISHED;
    this.publishedAt = Instant.now();
    this.nextAttemptAt = null;
    this.processingStartedAt = null;
    this.lastError = null;
  }

  public void markProcessing() {
    this.status = OutboxStatus.PROCESSING;
    this.processingStartedAt = Instant.now();
  }

  @SuppressWarnings("PMD.NullAssignment")
  public void markPublishFailed(
      String errorMessage, int maxAttempts, long baseBackoffMs, long maxBackoffMs) {
    this.attemptCount++;
    this.lastError = errorMessage;
    if (this.attemptCount >= maxAttempts) {
      this.status = OutboxStatus.DEAD;
      this.nextAttemptAt = null;
      this.processingStartedAt = null;
      return;
    }

    long multiplier = 1L << Math.min(this.attemptCount - 1, 30);
    long delayMs = Math.min(baseBackoffMs * multiplier, maxBackoffMs);
    this.status = OutboxStatus.PENDING;
    this.nextAttemptAt = Instant.now().plus(delayMs, ChronoUnit.MILLIS);
    this.processingStartedAt = null;
  }
}
