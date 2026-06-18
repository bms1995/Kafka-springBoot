# Load Testing

The load tests are k6 scripts that exercise the gateway order creation path.

## Profiles

- `tests/load/k6-smoke.js`: short baseline, 5 VUs for 1 minute.
- `tests/load/k6-spike.js`: ramp to 40 VUs to check burst behavior.
- `tests/load/k6-endurance.js`: longer run, configurable with `VUS` and `DURATION`.

## Commands

```powershell
k6 run .\tests\load\k6-smoke.js
k6 run .\tests\load\k6-spike.js
$env:VUS=10; $env:DURATION="10m"; k6 run .\tests\load\k6-endurance.js
```

## Default Thresholds

- smoke/endurance: p95 latency below 1 second, error rate below 2 percent
- spike: p95 latency below 1.5 seconds, error rate below 5 percent

## Interpreting Results

During a run, watch:

- gateway p95 latency
- HTTP 5xx rate
- `order_outbox_pending_events`
- `payment_outbox_pending_events`
- Kafka consumer lag
- Loki logs filtered by `service` or `trace_id`
