package com.example.paymentservice.event;

import com.example.events.InventoryFailedEvent;
import com.example.events.OrderCreatedEvent;

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
    public static EventMetadata from(OrderCreatedEvent source, String producer) {
        return next(source.getCorrelationId(), source.getEventId(), producer);
    }

    public static EventMetadata from(InventoryFailedEvent source, String producer) {
        return next(source.getCorrelationId(), source.getEventId(), producer);
    }

    private static EventMetadata next(String correlationId, String causationId, String producer) {
        return new EventMetadata(
                UUID.randomUUID().toString(),
                correlationId,
                causationId,
                Instant.now().toString(),
                producer,
                "1"
        );
    }
}
