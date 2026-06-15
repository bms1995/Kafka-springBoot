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
- Circuit breaker Resilience4j sur les routes de l'API Gateway
- Metrics Prometheus et visualisation Grafana
- Tracing distribue OTEL vers Jaeger
- Smoke test local
- CI GitHub Actions : schemas, tests Maven, build Docker
- Manifests Kubernetes de base pour les microservices applicatifs

## Securite
- Remplacer l'API key locale par OAuth2/OIDC ou JWT
- Activer HTTPS/TLS entre clients et gateway
- Stocker les secrets dans un secret manager, pas dans des variables locales simples
- Ajouter une rotation des secrets
- Restreindre les ports exposes publiquement
- Ajouter des checks de dependances vulnerables dans la CI

## Deploiement
- Industrialiser les manifests Kubernetes ou ajouter un chart Helm
- Ajouter readiness/liveness probes Kubernetes
- Externaliser la configuration par environnement : local, staging, production
- Ajouter un pipeline de deploiement vers un cluster
- Ajouter une strategie blue/green ou rolling update

## Resilience
- Ajouter circuit breaker et timeout sur les nouveaux appels HTTP interservices si de nouveaux appels synchrones sont introduits
- Ajouter des limites de consommation Kafka par service
- Ajouter une strategie de replay controle des DLQ
- Ajouter des tests de panne Kafka, PostgreSQL et Schema Registry
- Verifier les garanties d'ordre par cle Kafka pour les evenements d'une meme commande

## Observabilite
- Ajouter des dashboards Grafana versionnes dans le repo
- Ajouter des alertes Prometheus pour chaque service critique
- Centraliser les logs avec Loki ou ELK
- Ajouter des correlation ids dans tous les logs metier
- Documenter les traces Jaeger attendues pour un flux normal et un flux de compensation

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
1. Tests end-to-end Saga avec Testcontainers
2. Dashboards Grafana versionnes
3. Authentification JWT/OAuth2 sur l'API Gateway
4. Kubernetes ou Helm
5. Logs centralises
6. Tests de charge
