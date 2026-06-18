# Progressive Delivery Runbook

## Purpose

Use canary delivery for gateway-facing changes when Argo Rollouts and Prometheus are installed in the cluster.

The optional manifests live in `k8s/progressive` and are not part of the base deployment because they require Argo Rollouts CRDs.

## Rollout Flow

1. Deploy the new image tag.
2. Shift 10 percent of traffic.
3. Check 5xx rate and p95 latency from Prometheus.
4. Shift 50 percent of traffic if metrics stay below thresholds.
5. Promote to 100 percent or abort automatically on metric failure.

## Manual Commands

```powershell
kubectl apply -k k8s/progressive
kubectl -n kafka-spring argo rollouts get rollout api-gateway --watch
kubectl -n kafka-spring argo rollouts promote api-gateway
kubectl -n kafka-spring argo rollouts abort api-gateway
```

## Abort Criteria

- Gateway 5xx ratio exceeds 2 percent during analysis.
- Gateway p95 latency exceeds 1 second during analysis.
- Smoke test fails after rollout pause.
- Kafka consumer lag increases continuously after the gateway change.
