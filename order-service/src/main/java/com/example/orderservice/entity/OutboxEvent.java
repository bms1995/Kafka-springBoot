package com.example.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

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
    }
}
