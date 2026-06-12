# Event Catalog

This catalog is the source of truth for the local event-driven contracts used by the services.

Machine-readable contract documentation is available in `docs/asyncapi.yaml`.

## Topic Ownership

| Topic | Event | Producer | Consumers | DLQ |
| --- | --- | --- | --- | --- |
| `order-created` | `OrderCreatedEvent` | `order-service` | `payment-service` | `order-created.DLQ` |
| `payment-processed` | `PaymentProcessedEvent` | `payment-service` | `order-service`, `inventory-service` | `payment-processed.DLQ` |
| `payment-failed` | `PaymentFailedEvent` | `payment-service` | `order-service`, `notification-service` | `payment-failed.DLQ` |
| `payment-refunded` | `PaymentRefundedEvent` | `payment-service` | `order-service`, `notification-service` | `payment-refunded.DLQ` |
| `inventory-updated` | `InventoryUpdatedEvent` | `inventory-service` | `order-service`, `notification-service` | `inventory-updated.DLQ` |
| `inventory-failed` | `InventoryFailedEvent` | `inventory-service` | `order-service`, `payment-service` | `inventory-failed.DLQ` |

## Required Metadata

Every event must carry:

| Field | Purpose |
| --- | --- |
| `eventId` | Unique event identifier used for idempotency and audit. |
| `correlationId` | Stable saga identifier across the full workflow. |
| `causationId` | Parent event that caused this event. |
| `occurredAt` | Producer-side event timestamp. |
| `producer` | Service that created the event. |
| `schemaVersion` | Logical version of the event contract. |

## Contract Rules

- Event schemas live in each service under `src/main/avro`.
- When a schema is shared by multiple services, every copy must be identical.
- New optional fields should define Avro defaults.
- Removing or renaming fields is a breaking change.
- Topic names are configurable through `app.kafka.topics.*` and environment variables.
- Docker Compose provisions all business topics and matching `.DLQ` topics through `kafka-init`.

## Local Topic Provisioning

The local stack creates these topics with 3 partitions and replication factor 1:

```text
order-created
payment-processed
payment-failed
payment-refunded
inventory-updated
inventory-failed
order-created.DLQ
payment-processed.DLQ
payment-failed.DLQ
payment-refunded.DLQ
inventory-updated.DLQ
inventory-failed.DLQ
```

In a real cluster, replication factor should match the Kafka cluster policy, commonly 3 or more.
