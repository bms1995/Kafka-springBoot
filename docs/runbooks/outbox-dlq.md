# Outbox and DLQ Runbook

## Signals

- `OrderOutboxPublishFailures` or `PaymentOutboxPublishFailures`.
- `OrderDeadOutboxEvents` or `PaymentDeadOutboxEvents`.
- Kafka dead-letter topics receive new records.
- Saga status stops progressing.

## Triage

1. Identify the failing service and topic.
2. Check broker and Schema Registry availability.
3. Check the failing event payload, `eventId`, `correlationId` and `causationId`.
4. Verify whether the same `eventId` was already consumed through the inbox table.
5. Confirm whether failure is transient, schema-related or business-data-related.

## Remediation

- Transient broker failure: restore broker connectivity and let outbox retry.
- Schema compatibility failure: stop deployment, fix schema compatibility and redeploy.
- Poison event: move to dead outbox, document the reason, and replay only after correction.
- Duplicate event: do not replay unless idempotence is confirmed by `eventId`.

## Replay Rules

- Replay by `eventId` or `correlationId`, never by broad time window without review.
- Keep the original `eventId` for idempotent consumers.
- Record operator, reason, timestamp and affected order ids.
