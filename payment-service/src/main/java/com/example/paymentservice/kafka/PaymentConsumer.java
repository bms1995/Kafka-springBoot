package com.example.paymentservice.kafka;

import com.example.events.OrderCreatedEvent;
import com.example.events.InventoryFailedEvent;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.listeners.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-created", groupId = "payment-group")
    public void consume(OrderCreatedEvent event) {
        log.info("Received order-created event: {}", event);
        paymentService.processPayment(event);
    }

    @KafkaListener(topics = "inventory-failed", groupId = "payment-compensation-group")
    public void consumeInventoryFailure(InventoryFailedEvent event) {
        log.info("Received inventory-failed event: {}", event);
        paymentService.compensatePayment(event);
    }
}
