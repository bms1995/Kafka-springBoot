package com.example.orderservice.kafka;

import com.example.events.InventoryFailedEvent;
import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.events.PaymentRefundedEvent;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-processed", groupId = "order-saga-group")
    public void consumePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment-processed event: {}", event);
        orderService.markPaymentConfirmed(event.getOrderId());
    }

    @KafkaListener(topics = "payment-failed", groupId = "order-saga-group")
    public void consumePaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment-failed event: {}", event);
        orderService.markPaymentFailed(event.getOrderId(), event.getReason());
    }

    @KafkaListener(topics = "inventory-updated", groupId = "order-saga-group")
    public void consumeInventoryUpdated(InventoryUpdatedEvent event) {
        log.info("Received inventory-updated event: {}", event);
        orderService.markInventoryConfirmed(event.getOrderId());
    }

    @KafkaListener(topics = "inventory-failed", groupId = "order-saga-group")
    public void consumeInventoryFailed(InventoryFailedEvent event) {
        log.info("Received inventory-failed event: {}", event);
        orderService.markInventoryFailed(event.getOrderId(), event.getReason());
    }

    @KafkaListener(topics = "payment-refunded", groupId = "order-saga-group")
    public void consumePaymentRefunded(PaymentRefundedEvent event) {
        log.info("Received payment-refunded event: {}", event);
        orderService.markRefunded(event.getOrderId(), event.getReason());
    }
}
