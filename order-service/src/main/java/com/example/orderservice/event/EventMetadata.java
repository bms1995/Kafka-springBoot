package com.example.orderservice.event;

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
    public static EventMetadata start(String producer) {
        String eventId = UUID.randomUUID().toString();
        return new EventMetadata(
                eventId,
                eventId,
                eventId,
                Instant.now().toString(),
                producer,
                "1"
        );
    }
}
