# Logging Runbook

## Local Stack

Docker Compose includes Loki and Promtail for local centralized logs:

- Loki: `http://localhost:3100`
- Grafana datasource: `Loki`
- Promtail discovers Docker containers through the Docker socket.

## Useful Queries

```logql
{service="api-gateway"}
{service="order-service"} |= "smoke-success"
{application="payment-service"} |= "Payment processed"
{trace_id!=""}
```

## Incident Workflow

1. Start from the failing `orderId` or `correlationId`.
2. Search gateway logs for the incoming request.
3. Follow the same trace or correlation context through order, payment and inventory services.
4. Compare log timestamps with Kafka lag and outbox metrics.
5. If a DLQ is involved, inspect it with `scripts/kafka-dlq-preview.ps1`.
