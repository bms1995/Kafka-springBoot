package com.example.paymentservice.kafka;

import com.example.events.PaymentProcessedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    private static final String PAYMENT_REFUNDED_TOPIC = "payment-refunded";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(PaymentProcessedEvent event) {
        kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, event.getOrderId(), event);
        log.info("Published payment-processed event: {}", event);
    }

    public void sendFailure(PaymentFailedEvent event) {
        kafkaTemplate.send(PAYMENT_FAILED_TOPIC, event.getOrderId(), event);
        log.info("Published payment-failed event: {}", event);
    }

    public void sendRefund(PaymentRefundedEvent event) {
        kafkaTemplate.send(PAYMENT_REFUNDED_TOPIC, event.getOrderId(), event);
        log.info("Published payment-refunded compensation event: {}", event);
    }
}
