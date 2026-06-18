# Target Architecture

Ce document decrit la cible d'architecture pour faire evoluer le projet vers une plateforme microservices exploitable en staging puis production.

## Principes

- Un seul point d'entree HTTP : `api-gateway`
- Communication inter-services asynchrone via Kafka-compatible broker
- Bases separees par domaine metier
- Contrats evenementiels versionnes avec Avro et AsyncAPI
- Deploiements reproductibles par environnement avec Kustomize
- Secrets et configuration separes du code
- Observabilite obligatoire : logs correles, metriques, traces, alertes
- Ownership par domaine : code, base, contrats, SLO et runbooks appartiennent au service
- Graceful degradation : une panne partielle doit degrader le parcours sans bloquer toute la plateforme
- Images immuables et rollbacks par SHA

Voir aussi `docs/hyperscale-architecture.md` pour le track d'evolution inspire Zalando, Amazon et Netflix.

## Environnements

### Local

`overlays/local` cible Docker Desktop Kubernetes.

Il embarque l'infra minimale pour developper vite :

- PostgreSQL in-cluster
- Redpanda in-cluster
- Schema Registry integre Redpanda
- replicas et HPA limites a 1

### Staging

`overlays/staging` cible un cluster distant, par exemple K3s sur OVH.

Il deploie les workloads applicatifs avec les images GHCR. Les dependances doivent etre fournies par le cluster ou par un overlay dedie :

- PostgreSQL avec stockage persistant
- Kafka/Redpanda avec stockage persistant
- Ingress TLS
- secrets reels
- monitoring

## Domaines

- `api-gateway` : securite edge, routage, correlation id, rate limiting, circuit breakers
- `order-service` : orchestration Saga, read model commande, outbox, timeline d'evenements
- `payment-service` : paiement, idempotence, compensation remboursement, outbox
- `inventory-service` : reservation inventaire, refus inventaire
- `notification-service` : notifications sur evenements confirmes

## Contrats

Les evenements Kafka sont des contrats publics entre services.

Le module `common-events` porte les constantes et conventions partagees :

- noms canoniques des topics
- noms des producteurs
- champs de metadata obligatoires
- helpers de metadata generiques

Le module `common-kafka` porte les conventions techniques Kafka partagees :

- error handler standard
- routage dead-letter topic
- retry/backoff par defaut
- logging uniforme des tentatives de consommation

Regles cible :

- evolution Avro compatible
- `eventId`, `correlationId`, `causationId`, `occurredAt`, `producer`, `schemaVersion` obligatoires
- validation des schemas en CI
- documentation AsyncAPI maintenue avec les schemas
- tests de compatibilite producteur/consommateur

## Saga

`order-service` porte l'etat materialise de la Saga.

Ameliorations cible :

- state machine explicite
- timeout de Saga
- retry/replay controle
- dead-letter metier pour commandes bloquees
- endpoint admin protege pour inspection et remediation

## Outbox et Inbox

Les services qui publient des evenements doivent utiliser une outbox transactionnelle.

Ameliorations cible :

- compteur de tentatives
- backoff configurable
- dead outbox apres seuil d'echec
- retention/archivage
- idempotence cote consommateur via inbox indexee par `eventId`

## Securite

Local :

- API key acceptee pour simplifier le developpement

Staging/production :

- JWT/OIDC active
- TLS obligatoire
- secrets Kubernetes generes depuis GitHub Secrets ou secret manager
- NetworkPolicy couvrant apps, DB et broker
- aucun secret par defaut dans les overlays distants

## Observabilite

Chaque service doit exposer :

- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/actuator/prometheus`
- logs avec `correlationId`
- traces OTLP

SLO minimum :

- disponibilite gateway
- latence gateway p95
- taux de Saga en echec
- lag consommateur Kafka
- echec publication outbox

## Deploiement

Flux cible :

1. CI valide schemas, tests et scan securite
2. CI build et publie les images GHCR taguees par SHA
3. Workflow staging applique `overlays/staging`
4. Workflow pin les images au SHA deploye
5. Workflow attend les rollouts
6. Workflow execute un smoke test via `api-gateway`

Rollback :

- redeployer un tag SHA precedent
- ne jamais dependre de `latest` pour une remediation production

## Plateforme

La cible plateforme ajoute des garde-fous communs a tous les services :

- quotas et limites de namespace pour eviter qu'un service consomme tout le cluster
- priorite Kubernetes pour les workloads applicatifs critiques
- repartition des replicas sur plusieurs nodes quand le cluster le permet
- service catalog interne avec ownership, dependances, SLO, dashboards et runbooks
- validation policy-as-code des manifests avant deploiement
