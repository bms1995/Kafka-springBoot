# Kafka Spring Microservices

Projet local avec :
- `order-service`
- `payment-service`
- `inventory-service`
- `notification-service`
- Kafka, Schema Registry, Kafka UI
- PostgreSQL separe pour order, payment et inventory
- Prometheus, Grafana, Actuator
- Saga, Outbox, Avro, Flyway, anti double paiement

## Prerequis
- Java 21
- Maven 3.9+
- Docker Desktop

## Lancer l'infra
```bash
docker compose up -d
```

URLs utiles :
- Kafka UI : http://localhost:8085
- Schema Registry : http://localhost:8086
- Prometheus : http://localhost:9090
- Grafana : http://localhost:3000
- PgAdmin : http://localhost:5050

Grafana local :
- login : `admin`
- password : `admin`

## Lancer les services
Lance les services dans 4 terminaux separes, dans cet ordre :

```bash
cd payment-service
mvn spring-boot:run
```

```bash
cd inventory-service
mvn spring-boot:run
```

```bash
cd notification-service
mvn spring-boot:run
```

```bash
cd order-service
mvn spring-boot:run
```

## Tester avec PowerShell
Succes normal :

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/orders" `
  -ContentType "application/json" `
  -Body '{"orderId":"order-123","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'
```

Echec paiement :

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/orders" `
  -ContentType "application/json" `
  -Body '{"orderId":"order-fail-payment","productName":"MacBook Pro","quantity":1,"amount":999,"customerEmail":"client@test.com"}'
```

Echec inventory + compensation :

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/orders" `
  -ContentType "application/json" `
  -Body '{"orderId":"fail-inventory-1","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'
```

Consulter l'etat materialise d'une commande :

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8081/api/orders/order-123"
```

Consulter la timeline d'evenements d'une commande :

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8081/api/orders/order-123/events"
```

Smoke test complet :

```powershell
.\scripts\smoke-test.ps1
```

## Flux Saga
1. `order-service` persiste la commande et enqueue `order-created` dans son outbox
2. `payment-service` consomme `order-created`
3. Si paiement OK, `payment-service` publie `payment-processed`
4. `inventory-service` consomme `payment-processed`
5. Si inventory OK, `inventory-service` publie `inventory-updated`
6. `order-service` consomme les evenements payment/inventory et met a jour son read model
7. `notification-service` consomme `inventory-updated`

Compensations :
- paiement refuse : `payment-failed`
- inventory indisponible : `inventory-failed`
- compensation paiement : `payment-refunded`

Statuts de commande materialises par `order-service` :
- `CREATED`
- `PAYMENT_CONFIRMED`
- `PAYMENT_FAILED`
- `INVENTORY_CONFIRMED`
- `INVENTORY_FAILED`
- `REFUNDED`

## Inbox et Audit Trail
`order-service` garde une trace des evenements de saga recus :
- table `processed_events` pour ignorer les doublons Kafka
- table `order_event_history` pour reconstruire la timeline metier
- id d'evenement derive de `topic-partition-offset`
- endpoint `GET /api/orders/{orderId}/events`

Cette approche simule un pattern inbox/read-model utilise en production pour debug, replay controle et audit.

## Anti Double Paiement
`payment-service` combine :
- `orderId` comme idempotency key
- table `payment_transactions` avec `order_id` unique
- table `processed_events`
- transaction Spring
- transactional outbox dans `outbox_events`
- producer Kafka idempotent
- compteur `payment_duplicate_skipped_total`

## Transactional Outbox
`order-service` et `payment-service` utilisent une table `outbox_events` :
- l'etat metier et l'evenement sont ecrits dans la meme transaction DB
- un publisher planifie publie les evenements pending vers Kafka
- si Kafka est indisponible, l'evenement reste en base et sera rejoue
- les publishers exposent des compteurs Prometheus de succes/echec

## Avro et Schema Registry
Les evenements Kafka utilisent Avro :
- schemas dans `src/main/avro`
- classes Java generees par `avro-maven-plugin`
- `KafkaAvroSerializer`
- `KafkaAvroDeserializer`
- namespace partage : `com.example.events`
- Schema Registry : `SCHEMA_REGISTRY_URL`

## Observabilite
Health :
- order-service : http://localhost:8081/actuator/health
- payment-service : http://localhost:8082/actuator/health
- notification-service : http://localhost:8083/actuator/health
- inventory-service : http://localhost:8084/actuator/health

Metrics :
- `/actuator/prometheus` sur chaque service
- metriques payment/outbox :
  - `order_outbox_published_total`
  - `order_outbox_publish_failed_total`
  - `payment_succeeded_total`
  - `payment_failed_total`
  - `payment_duplicate_skipped_total`
  - `payment_refunded_total`
  - `payment_outbox_published_total`
  - `payment_outbox_publish_failed_total`

Alertes Prometheus :
- `monitoring/alerts.yml`

## Resilience Kafka
Les consommateurs critiques utilisent un `DefaultErrorHandler` Spring Kafka :
- 3 tentatives de retry
- pause de 2 secondes entre les tentatives
- publication du message en echec vers un topic dead-letter `nom-du-topic.DLQ`
- logs de chaque tentative avec topic, cle et payload

Services couverts :
- `order-service`
- `payment-service`
- `inventory-service`
- `notification-service`

## Migrations DB
Flyway gere les schemas :
- `order-service/src/main/resources/db/migration/V1__init_order_schema.sql`
- `order-service/src/main/resources/db/migration/V2__add_order_read_model_columns.sql`
- `order-service/src/main/resources/db/migration/V3__add_order_inbox_and_event_history.sql`
- `payment-service/src/main/resources/db/migration/V1__init_payment_schema.sql`
- `inventory-service/src/main/resources/db/migration/V1__init_inventory_schema.sql`

Hibernate est en `ddl-auto: validate`.

## Tests
```bash
cd payment-service
mvn test
```

```bash
cd inventory-service
mvn test
```

Le test Testcontainers de `payment-service` utilise PostgreSQL reel quand Docker est accessible. Sinon il est ignore automatiquement.

## Reset Local
Supprime les donnees locales Docker :

```bash
docker compose down -v
docker compose up -d
```

Attention : cette commande supprime les volumes PostgreSQL locaux.
