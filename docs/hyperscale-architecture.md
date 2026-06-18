# Hyperscale Architecture Track

Ce document traduit des pratiques observees chez des acteurs comme Zalando, Amazon et Netflix en objectifs realistes pour ce projet. Le but n'est pas de copier leur taille d'organisation, mais de pousser le repo vers des principes exploitables : autonomie des domaines, contrats stricts, resilience par defaut, observabilite actionnable et deploiements repetables.

## Principes cibles

- Ownership par domaine : chaque service possede son code, sa base, ses migrations, ses contrats et ses SLO.
- API-first et event-first : les interfaces HTTP et Kafka sont des produits internes versionnes.
- You build it, you run it : les services exposent assez de metriques, logs, traces et runbooks pour etre operes par l'equipe qui les maintient.
- Cell-based architecture : le trafic peut etre isole par cellule, region ou tenant quand le systeme grandit.
- Graceful degradation : une panne d'un domaine degrade le parcours au lieu de bloquer toute la plateforme.
- Automated rollback : chaque release doit pouvoir revenir a une image precedente taguee par SHA.

## Niveau 1: production discipline

Ce niveau correspond a une plateforme staging/prod robuste.

- Images immuables taguees par commit SHA.
- Readiness/liveness probes sur tous les services.
- Requests, limits, HPA et PodDisruptionBudget.
- NetworkPolicy avec ingress minimal et egress maitrise.
- Secrets geres hors Git, injectes par environnement.
- Alertes sur disponibilite, latence, erreurs, lag Kafka et outbox bloquee.
- Runbooks pour DLQ, outbox, replay Saga et rollback.

## Niveau 2: platform engineering

Ce niveau rapproche le projet d'une plateforme interne mature.

- Golden path de creation de service : template Spring Boot, Kafka, Actuator, OTEL, Dockerfile, K8s et CI.
- Service catalog : ownership, dependances, SLO, topics consommes/produits, dashboards et runbooks.
- Policy as code : validation Kubernetes, controle des privileges, images signees et SBOM.
- Progressive delivery : canary ou blue/green avec metriques de rollback.
- Contract testing : compatibilite Avro et tests consommateur/producteur dans la CI.
- Incident readiness : postmortems, budgets d'erreur et exercices de panne.
- Admission control : policies Kubernetes auditees puis bloquees pour les exigences runtime.

## Niveau 3: hyperscale patterns

Ces patterns ne sont utiles que si le trafic ou l'organisation le justifie.

- Partitionnement par cellule : plusieurs piles applicatives identiques, chacune avec ses brokers, bases et quotas.
- Multi-region active/passive puis active/active pour les domaines critiques.
- Routage par tenant ou region au niveau gateway.
- Backpressure explicite : limites de consommation Kafka, file d'attente par priorite et degradation controlee.
- Replay industriel : outils admin audites pour rejouer DLQ/outbox par fenetre, topic, cle ou correlationId.
- Schema governance : revue automatique de compatibilite, deprecation policy et documentation AsyncAPI publiee.
- Partitioning explicite : cle metier stable, ordering par agregat et consumer groups supervises.

## Mapping vers ce repo

| Inspiration | Traduction dans le repo |
| --- | --- |
| Zalando platform engineering | Kustomize overlays, ownership par service, Kubernetes policies, Postgres par domaine |
| Amazon two-pizza/domain ownership | Services bornes par domaine et bases separees |
| Netflix resilience engineering | Circuit breakers, timeouts, graceful degradation, chaos tests futurs |
| Event-driven commerce | Kafka topics versionnes, Avro, outbox/inbox, Saga materialisee |

## Prochaines etapes recommandees

1. Ajouter des tests end-to-end Saga avec Kafka/PostgreSQL Testcontainers multi-services.
2. Versionner des dashboards Grafana par service et un dashboard Saga.
3. Executer et publier les resultats k6 de pic et endurance.
4. Ajouter un pipeline GitOps avec Argo CD ou Flux.
5. Passer les policies Kyverno de `Audit` a `Enforce` quand le cluster est pret.
