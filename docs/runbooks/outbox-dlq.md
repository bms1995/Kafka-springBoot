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

## Controlled Replay

Preview one matching record without publishing anything:

```powershell
.\scripts\kafka-dlq-replay.ps1 -Topic order-created.DLQ -Key order-123
```

After correcting the root cause and validating idempotence, execute the replay:

```powershell
.\scripts\kafka-dlq-replay.ps1 `
  -Topic order-created.DLQ `
  -Key order-123 `
  -Execute `
  -Operator "alice" `
  -Reason "INC-42 schema fix deployed"
```

The tool copies raw key/value bytes and existing headers, so Avro payloads are not decoded or
re-encoded. It adds source offset and operator audit headers. Execution always requires an exact
key, operator and reason; replay is limited to one record by default and 100 maximum.
