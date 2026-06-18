# Saga Operations Runbook

## Signals

- `order-service` readiness is down.
- Orders stay in `CREATED` or `PAYMENT_PROCESSED` longer than the expected Saga window.
- `KafkaConsumerLagHigh` fires for payment or inventory topics.
- Customer support reports inconsistent order state.

## Triage

1. Check `api-gateway` and `order-service` readiness.
2. Query `GET /api/orders/{orderId}` through the gateway.
3. Query `GET /api/orders/{orderId}/events` and verify the last Saga event.
4. Check Kafka consumer lag for `payment-service`, `inventory-service` and `order-service`.
5. Check outbox alerts for `order-service` and `payment-service`.

## Remediation

- If payment failed, confirm the order status is `PAYMENT_FAILED` and no inventory reservation happened.
- If inventory failed, confirm `payment-service` emitted `payment-refunded` and order status is `REFUNDED`.
- If a consumer was down, restore the deployment and let Kafka replay from committed offsets.
- If an outbox is dead, inspect the dead event payload before replaying.

## Escalation

Escalate to the owning domain when the timeline contains duplicate, missing or out-of-order business events for the same `orderId`.
