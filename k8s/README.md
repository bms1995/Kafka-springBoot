# Kubernetes Manifests

Ce dossier fournit un socle Kubernetes pour les microservices applicatifs.

Il ne deploie pas Kafka, PostgreSQL, Schema Registry, Prometheus, Grafana ou Jaeger. En production, ces composants sont generalement fournis par des operators ou services manages :

- Kafka : Strimzi, Confluent Cloud, AWS MSK
- PostgreSQL : Cloud SQL, RDS, Azure Database, Zalando Postgres Operator
- Observabilite : Prometheus Operator, Grafana Operator, OpenTelemetry Operator

## Appliquer les manifests

Adapter d'abord `secret.example.yaml` avec les valeurs de l'environnement cible.

```bash
kubectl apply -k k8s
```

Pour utiliser les images publiees dans GitHub Container Registry :

```bash
kubectl apply -k overlays/staging
```

Le workflow GitHub Actions `Deploy staging` peut appliquer cet overlay sur un cluster distant si le secret `KUBE_CONFIG_STAGING` est configure.

Pour Docker Desktop avec dependances embarquees :

```bash
kubectl apply -k overlays/local
```

`overlays/staging` attend que les services `postgres-order`, `postgres-payment`, `postgres-inventory`, `kafka`, `schema-registry` et `otel-collector` soient fournis par l'environnement cible ou par un overlay infra dedie.

## Exposer le gateway en local

```bash
kubectl -n kafka-spring port-forward svc/api-gateway 8080:8080
```

Puis tester :

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/orders" `
  -Headers @{"X-API-Key"="local-dev-key"} `
  -ContentType "application/json" `
  -Body '{"orderId":"k8s-demo-1","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'
```

## Ce que ces manifests apportent

- Namespace dedie
- ConfigMap commune
- Secret separe pour credentials et API key
- Deployments avec 2 replicas par service
- Services internes stables
- Probes readiness/liveness basees sur Spring Boot Actuator
- Requests/limits CPU et memoire
- Gateway expose en `LoadBalancer`
- Autoscaling horizontal avec HPA de 2 a 5 replicas par service
- NetworkPolicy pour limiter le trafic entrant vers les services
- PodDisruptionBudget pour garder au moins 1 pod disponible par service pendant les maintenances
- ResourceQuota et LimitRange pour cadrer la consommation CPU/memoire du namespace
- PriorityClass applicative pour les workloads critiques
- Topology spread constraints pour repartir les replicas quand plusieurs nodes sont disponibles

## Add-ons optionnels

Ces dossiers exigent des operators/CRD supplementaires et ne sont pas inclus dans le socle `k8s` :

- `k8s/observability` : `ServiceMonitor` et `PrometheusRule` pour Prometheus Operator
- `k8s/progressive` : canary `Rollout` et `AnalysisTemplate` pour Argo Rollouts
- `k8s/policies` : policies Kyverno en mode `Audit` pour durcissement runtime et ressources

## A faire pour une vraie production

- Remplacer `image:local` par des images taguees dans un registry
- Deployer l'overlay `overlays/staging` depuis un runner connecte au cluster
- Utiliser un secret manager externe
- Ajouter Ingress avec TLS
- Ajouter ServiceMonitor Prometheus Operator
- Ajouter validation policy-as-code des manifests et admission control
