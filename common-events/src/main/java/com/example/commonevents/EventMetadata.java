package com.example.commonevents;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EventMetadata(
        String eventId,
        String correlationId,
        String causationId,
        Instant occurredAt,
        String producer,
        String schemaVersion
) {

    public EventMetadata {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(causationId, "causationId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(producer, "producer is required");
        Objects.requireNonNull(schemaVersion, "schemaVersion is required");
    }

    public static EventMetadata createRoot(String producer) {
        String eventId = UUID.randomUUID().toString();
        return new EventMetadata(eventId, eventId, eventId, Instant.now(), producer, "1");
    }

    public static EventMetadata createChild(String producer, String correlationId, String causationId) {
        return new EventMetadata(UUID.randomUUID().toString(), correlationId, causationId, Instant.now(), producer, "1");
    }
}
