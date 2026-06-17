package com.example.paymentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private String eventId;

    private String aggregateId;

    private String topic;

    private String eventType;

    @Column(columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private Instant createdAt;

    private Instant publishedAt;

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
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void markPublishFailed(String errorMessage, int maxAttempts) {
        this.attemptCount++;
        this.lastError = errorMessage;
        if (this.attemptCount >= maxAttempts) {
            this.status = OutboxStatus.DEAD;
        }
    }
}
