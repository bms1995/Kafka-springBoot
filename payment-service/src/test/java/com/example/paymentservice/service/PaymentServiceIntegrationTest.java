package com.example.paymentservice.service;

import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.events.OrderCreatedEvent;
import com.example.paymentservice.repository.OutboxEventRepository;
import com.example.paymentservice.repository.PaymentTransactionRepository;
import com.example.paymentservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.outbox.publish-delay-ms=600000",
        "spring.kafka.bootstrap-servers=localhost:19092"
})
class PaymentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("paymentdb")
            .withUsername("myuser")
            .withPassword("mypassword");

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void processPaymentPersistsTransactionProcessedEventAndOutboxEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "order-it-1",
                "Laptop",
                1,
                BigDecimal.valueOf(250).toPlainString(),
                "customer@example.com"
        );

        paymentService.processPayment(event);
        paymentService.processPayment(event);

        assertThat(paymentTransactionRepository.findById(event.getOrderId()))
                .get()
                .extracting(transaction -> transaction.getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(processedEventRepository.existsById(event.getOrderId())).isTrue();
        assertThat(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .hasSize(1)
                .first()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.getAggregateId()).isEqualTo(event.getOrderId());
                    assertThat(outboxEvent.getTopic()).isEqualTo(PaymentOutboxService.PAYMENT_PROCESSED_TOPIC);
                });
    }
}
