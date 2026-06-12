package com.example.inventoryservice.event;

import com.example.events.PaymentProcessedEvent;

import java.time.Instant;
import java.util.UUID;

public record EventMetadata(
        String eventId,
        String correlationId,
        String causationId,
        String occurredAt,
        String producer,
        String schemaVersion
) {
    public static EventMetadata from(PaymentProcessedEvent source, String producer) {
        return new EventMetadata(
                UUID.randomUUID().toString(),
                source.getCorrelationId(),
                source.getEventId(),
                Instant.now().toString(),
                producer,
                "1"
        );
    }
}
