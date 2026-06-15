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

## A faire pour une vraie production

- Remplacer `image:local` par des images taguees dans un registry
- Utiliser un secret manager externe
- Ajouter Ingress avec TLS
- Ajouter ServiceMonitor Prometheus Operator
