# Kafka Operations

## Topic Strategy

Business topics use three partitions in local Docker Compose:

- `order-created`
- `payment-processed`
- `payment-failed`
- `payment-refunded`
- `inventory-updated`
- `inventory-failed`

Each topic also has a `.DLQ` counterpart.

## Partition Key

Producers use the order aggregate id as the Kafka record key:

- `OrderOutboxPublisher` sends `aggregateId` to `order-created`.
- `PaymentOutboxPublisher` sends `aggregateId` to payment events.
- Inventory events carry the same order context downstream.

This means events for the same order are routed to the same partition per topic. Kafka only guarantees ordering within one partition of one topic, so the Saga must still be idempotent and state-aware across topics.

## Consumer Groups

- `payment-group` consumes `order-created`.
- `payment-compensation-group` consumes `inventory-failed`.
- `inventory-group` consumes `payment-processed`.
- `order-service` consumes payment and inventory outcomes.
- `notification-service` consumes final customer-facing outcomes.

## Replay Rules

- Prefer replay by `eventId`, `orderId` or `correlationId`.
- Keep the original `eventId` so inbox tables can prevent duplicate side effects.
- Never replay a whole DLQ without checking schema compatibility and business impact.
- Record operator, reason, source topic, target topic and affected keys.

## Retention Guidance

Local defaults are intentionally simple. Production should define:

- retention per topic based on audit needs
- compaction only for state topics, not immutable event history
- DLQ retention long enough for incident triage
- alerts on lag and DLQ growth
