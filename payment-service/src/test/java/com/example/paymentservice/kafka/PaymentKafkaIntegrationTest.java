package com.example.paymentservice.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.events.OrderCreatedEvent;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.repository.OutboxEventRepository;
import com.example.paymentservice.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "app.outbox.publish-delay-ms=600000")
class PaymentKafkaIntegrationTest {

  private static final Network NETWORK = Network.newNetwork();

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("paymentdb")
          .withUsername("myuser")
          .withPassword("mypassword");

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.9.1"))
          .withNetwork(NETWORK)
          .withNetworkAliases("kafka")
          .withListener("kafka:19092");

  @Container
  static final GenericContainer<?> SCHEMA_REGISTRY =
      new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.6.1"))
          .withNetwork(NETWORK)
          .withExposedPorts(8081)
          .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
          .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
          .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
          .dependsOn(KAFKA);

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private PaymentTransactionRepository paymentTransactionRepository;
  @Autowired private OutboxEventRepository outboxEventRepository;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    registry.add(
        "spring.kafka.consumer.properties.schema.registry.url",
        PaymentKafkaIntegrationTest::schemaRegistryUrl);
    registry.add(
        "spring.kafka.producer.properties.schema.registry.url",
        PaymentKafkaIntegrationTest::schemaRegistryUrl);
  }

  private static String schemaRegistryUrl() {
    return "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getMappedPort(8081);
  }

  @Test
  void consumesRealAvroEventAndPersistsPaymentAndOutbox() throws Exception {
    String orderId = "kafka-it-order-1";
    OrderCreatedEvent event =
        new OrderCreatedEvent(
            orderId,
            "Laptop",
            1,
            BigDecimal.valueOf(250).toPlainString(),
            "customer@example.com",
            "kafka-it-event-1",
            "kafka-it-correlation-1",
            "kafka-it-causation-1",
            "2026-06-19T12:00:00Z",
            "order-service",
            "1");

    kafkaTemplate.send("order-created", orderId, event).get();

    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () -> {
              assertThat(paymentTransactionRepository.findById(orderId))
                  .get()
                  .extracting(transaction -> transaction.getStatus())
                  .isEqualTo(PaymentStatus.SUCCESS);
              assertThat(
                      outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(
                          OutboxStatus.PENDING))
                  .anySatisfy(outbox -> assertThat(outbox.getAggregateId()).isEqualTo(orderId));
            });
  }

  @Test
  void publishesPermanentlyFailingEventToDeadLetterTopic() throws Exception {
    String orderId = "kafka-it-invalid-order";
    OrderCreatedEvent invalidEvent =
        new OrderCreatedEvent(
            orderId,
            "Laptop",
            1,
            "not-a-number",
            "customer@example.com",
            "kafka-it-invalid-event",
            "kafka-it-invalid-correlation",
            "kafka-it-invalid-causation",
            "2026-06-19T12:00:00Z",
            "order-service",
            "1");

    try (Consumer<String, Object> consumer = deadLetterConsumer()) {
      consumer.subscribe(List.of("order-created.DLQ"));
      kafkaTemplate.send("order-created", orderId, invalidEvent).get();

      ConsumerRecord<String, Object> deadLetter =
          KafkaTestUtils.getSingleRecord(consumer, "order-created.DLQ", Duration.ofSeconds(20));

      assertThat(deadLetter.key()).isEqualTo(orderId);
      assertThat(deadLetter.value()).isEqualTo(invalidEvent);
      assertThat(paymentTransactionRepository.existsById(orderId)).isFalse();
    }
  }

  private static Consumer<String, Object> deadLetterConsumer() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-test-" + UUID.randomUUID());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "io.confluent.kafka.serializers.KafkaAvroDeserializer");
    properties.put("schema.registry.url", schemaRegistryUrl());
    properties.put("specific.avro.reader", true);
    return new DefaultKafkaConsumerFactory<String, Object>(properties).createConsumer();
  }
}
