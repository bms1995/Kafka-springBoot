# Local Overlay

Cet overlay cible Docker Desktop Kubernetes.

Il deploie les microservices avec les images GHCR et ajoute l'infra locale minimale :

- PostgreSQL pour order, payment et inventory
- Redpanda comme broker Kafka-compatible
- Schema Registry integre de Redpanda
- service stub `otel-collector`

Les replicas et HPA sont limites a 1 pour eviter de saturer le noeud Docker Desktop.

```powershell
.\scripts\deploy-local.ps1 -ImageTag latest
```

Pour tester :

```powershell
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
```
