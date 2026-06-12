package com.example.paymentservice.kafka;

import com.example.events.PaymentProcessedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.payment-processed:payment-processed}")
    private String paymentProcessedTopic;

    @Value("${app.kafka.topics.payment-failed:payment-failed}")
    private String paymentFailedTopic;

    @Value("${app.kafka.topics.payment-refunded:payment-refunded}")
    private String paymentRefundedTopic;

    public void send(PaymentProcessedEvent event) {
        kafkaTemplate.send(paymentProcessedTopic, event.getOrderId(), event);
        log.info("Published payment-processed event: {}", event);
    }

    public void sendFailure(PaymentFailedEvent event) {
        kafkaTemplate.send(paymentFailedTopic, event.getOrderId(), event);
        log.info("Published payment-failed event: {}", event);
    }

    public void sendRefund(PaymentRefundedEvent event) {
        kafkaTemplate.send(paymentRefundedTopic, event.getOrderId(), event);
        log.info("Published payment-refunded compensation event: {}", event);
    }
}
