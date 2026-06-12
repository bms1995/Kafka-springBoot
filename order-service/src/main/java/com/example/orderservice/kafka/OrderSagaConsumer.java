package com.example.orderservice.kafka;

import com.example.events.InventoryFailedEvent;
import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.events.PaymentRefundedEvent;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.OrderEventEnvelope;
import com.example.orderservice.service.OrderEventPayloadSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaConsumer {

    private final OrderService orderService;
    private final OrderEventPayloadSerializer payloadSerializer;

    @KafkaListener(topics = "payment-processed", groupId = "order-saga-group")
    public void consumePaymentProcessed(
            PaymentProcessedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received payment-processed event: {}", event);
        orderService.markPaymentConfirmed(envelope(event, event.getOrderId(), topic, partition, offset));
    }

    @KafkaListener(topics = "payment-failed", groupId = "order-saga-group")
    public void consumePaymentFailed(
            PaymentFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received payment-failed event: {}", event);
        orderService.markPaymentFailed(envelope(event, event.getOrderId(), topic, partition, offset), event.getReason());
    }

    @KafkaListener(topics = "inventory-updated", groupId = "order-saga-group")
    public void consumeInventoryUpdated(
            InventoryUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received inventory-updated event: {}", event);
        orderService.markInventoryConfirmed(envelope(event, event.getOrderId(), topic, partition, offset));
    }

    @KafkaListener(topics = "inventory-failed", groupId = "order-saga-group")
    public void consumeInventoryFailed(
            InventoryFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received inventory-failed event: {}", event);
        orderService.markInventoryFailed(envelope(event, event.getOrderId(), topic, partition, offset), event.getReason());
    }

    @KafkaListener(topics = "payment-refunded", groupId = "order-saga-group")
    public void consumePaymentRefunded(
            PaymentRefundedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received payment-refunded event: {}", event);
        orderService.markRefunded(envelope(event, event.getOrderId(), topic, partition, offset), event.getReason());
    }

    private OrderEventEnvelope envelope(
            SpecificRecordBase event,
            String orderId,
            String topic,
            int partition,
            long offset
    ) {
        return new OrderEventEnvelope(
                topic + "-" + partition + "-" + offset,
                orderId,
                event.getClass().getSimpleName(),
                topic,
                payloadSerializer.toJson(event)
        );
    }
}
