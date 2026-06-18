# Rollback Runbook

## Preconditions

- Images are tagged by immutable commit SHA.
- The previous healthy SHA is known from the deployment history.
- Database migrations are backward compatible or have an explicit rollback plan.

## Kubernetes Rollback

1. Identify the last healthy image SHA.
2. Pin each affected deployment to that SHA.
3. Wait for rollout status.
4. Run `scripts/smoke-test.ps1` against the gateway.
5. Watch error rate, latency and Kafka lag for at least one Saga window.

## Commands

```powershell
kubectl -n kafka-spring set image deployment/payment-service payment-service=ghcr.io/bms1995/kafka-springboot/payment-service:<sha>
kubectl -n kafka-spring rollout status deployment/payment-service --timeout=180s
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
```

## Stop Conditions

- Rollback introduces schema incompatibility.
- Consumer lag keeps increasing after rollback.
- Gateway error rate remains above the rollback threshold.
