package com.example.notificationservice.kafka;

import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentRefundedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationConsumer {

    @KafkaListener(topics = "inventory-updated", groupId = "notification-group")
    public void consume(InventoryUpdatedEvent event) {

        log.info("Received inventory-updated event: {}", event);

        log.info("Sending email for orderId={}", event.getOrderId());
    }

    @KafkaListener(topics = "payment-failed", groupId = "notification-payment-failed-group")
    public void consumePaymentFailure(PaymentFailedEvent event) {
        log.info("Received payment-failed event: {}", event);
        log.info("Sending payment failure email for orderId={} reason={}",
                event.getOrderId(),
                event.getReason());
    }

    @KafkaListener(topics = "payment-refunded", groupId = "notification-payment-refunded-group")
    public void consumePaymentRefund(PaymentRefundedEvent event) {
        log.info("Received payment-refunded event: {}", event);
        log.info("Sending payment refund email for orderId={} reason={}",
                event.getOrderId(),
                event.getReason());
    }
}
