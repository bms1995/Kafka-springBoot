# Production Readiness Roadmap

Ce projet demontre une architecture microservices event-driven en local. Pour se rapprocher d'un environnement de production type e-commerce, les points suivants restent a renforcer.

## Deja couvert
- Microservices separes par domaine : gateway, order, payment, inventory, notification
- Communication asynchrone avec Kafka
- Saga avec compensation paiement
- Transactional outbox pour `order-service` et `payment-service`
- Idempotence paiement avec contrainte unique sur `orderId`
- Contrats Avro et Schema Registry
- PostgreSQL separe par domaine
- Migrations Flyway
- Dead-letter topics Kafka
- Retry Kafka cote consommateurs
- API Gateway avec API key, correlation id et rate limiting
- Support JWT/OAuth2 optionnel sur l'API Gateway
- Circuit breaker Resilience4j sur les routes de l'API Gateway
- Metrics Prometheus et visualisation Grafana
- Logs centralises locaux avec Loki/Promtail et datasource Grafana
- Tracing distribue OTEL vers Jaeger
- Smoke test local
- Smoke test E2E Saga via gateway avec verification des statuts et timelines metier
- Workflow E2E Docker Compose manuel/PR pour lancer la stack et executer le smoke Saga
- Test de charge k6 court avec seuils p95 et taux d'erreur
- CI GitHub Actions : schemas, tests Maven, build Docker
- CI GitHub Actions : rendu Kustomize base, local et staging
- Scan Trivy CI : vulnerabilites, secrets et mauvaises configurations
- Manifests Kubernetes de base pour les microservices applicatifs
- HorizontalPodAutoscaler Kubernetes sur les microservices applicatifs
- NetworkPolicy Kubernetes pour controler le trafic entre pods
- PodDisruptionBudget Kubernetes pour limiter les interruptions volontaires
- ResourceQuota et LimitRange Kubernetes pour cadrer la consommation du namespace applicatif
- PriorityClass et topology spread constraints pour ameliorer la disponibilite des workloads applicatifs
- Add-ons optionnels pour Prometheus Operator, Argo Rollouts et Kyverno

## Securite
- Remplacer progressivement l'API key locale par OAuth2/OIDC ou JWT
- Activer HTTPS/TLS entre clients et gateway
- Stocker les secrets dans un secret manager, pas dans des variables locales simples
- Ajouter une rotation des secrets
- Restreindre les ports exposes publiquement
- Ajouter des checks de dependances vulnerables dans la CI

## Deploiement
- Industrialiser les manifests Kubernetes ou ajouter un chart Helm
- Ajouter ServiceMonitor/PrometheusRule pour les clusters avec Prometheus Operator
- Ajouter ServiceMonitor/PrometheusRule pour les clusters avec Prometheus Operator
- Externaliser la configuration par environnement : local, staging, production
- Ajouter un pipeline de deploiement vers un cluster
- Ajouter une strategie blue/green ou rolling update
- Activer Argo Rollouts sur staging pour canary avec rollback automatique sur metriques

## Resilience
- Ajouter circuit breaker et timeout sur les nouveaux appels HTTP interservices si de nouveaux appels synchrones sont introduits
- Ajouter des limites de consommation Kafka par service
- Ajouter une strategie de replay controle des DLQ
- Documenter la strategie de partitioning Kafka par `orderId`
- Ajouter des tests de panne Kafka, PostgreSQL et Schema Registry
- Verifier les garanties d'ordre par cle Kafka pour les evenements d'une meme commande

## Observabilite
- Ajouter des dashboards Grafana versionnes dans le repo
- Ajouter des alertes Prometheus pour chaque service critique
- Industrialiser les logs centralises en staging avec Loki ou ELK
- Ajouter des correlation ids dans tous les logs metier
- Documenter les traces Jaeger attendues pour un flux normal et un flux de compensation
- Ajouter un service catalog avec ownership, SLO, runbooks et dependances par service

## Tests
- Ajouter des tests d'integration end-to-end sur la Saga complete
- Ajouter des tests Testcontainers pour Kafka, PostgreSQL et Schema Registry
- Ajouter des tests de contrats Avro backward/forward compatibility
- Ajouter des tests de charge avec k6 ou Gatling
- Ajouter des tests de non-regression pour l'idempotence paiement

## Donnees
- Ajouter une strategie de sauvegarde PostgreSQL
- Ajouter une politique de retention Kafka
- Ajouter une politique d'archivage des evenements metier
- Ajouter un plan de migration de schemas Avro

## Priorite recommandee
1. Tests end-to-end Saga avec Testcontainers multi-services
2. Dashboards Grafana versionnes
3. Authentification JWT/OAuth2 sur l'API Gateway
4. Activer progressive delivery canary sur staging
5. Logs centralises
6. Executer et documenter les resultats des profils k6 smoke, spike et endurance
