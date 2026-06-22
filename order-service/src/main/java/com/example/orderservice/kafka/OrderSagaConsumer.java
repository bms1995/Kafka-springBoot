package com.example.orderservice.kafka;

import com.example.events.InventoryFailedEvent;
import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.events.PaymentRefundedEvent;
import com.example.orderservice.service.OrderEventEnvelope;
import com.example.orderservice.service.OrderEventPayloadSerializer;
import com.example.orderservice.service.OrderService;
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

  private static final String CONSUMER_GROUP = "order-saga-group";

  private final OrderService orderService;
  private final OrderEventPayloadSerializer payloadSerializer;

  @KafkaListener(
      topics = "${app.kafka.topics.payment-processed:payment-processed}",
      groupId = CONSUMER_GROUP)
  public void consumePaymentProcessed(
      PaymentProcessedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {
    log.info("Received payment-processed event: {}", event);
    orderService.markPaymentConfirmed(
        envelope(event, event.getEventId(), event.getOrderId(), topic, partition, offset));
  }

  @KafkaListener(
      topics = "${app.kafka.topics.payment-failed:payment-failed}",
      groupId = CONSUMER_GROUP)
  public void consumePaymentFailed(
      PaymentFailedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {
    log.info("Received payment-failed event: {}", event);
    orderService.markPaymentFailed(
        envelope(event, event.getEventId(), event.getOrderId(), topic, partition, offset),
        event.getReason());
  }

  @KafkaListener(
      topics = "${app.kafka.topics.inventory-updated:inventory-updated}",
      groupId = CONSUMER_GROUP)
  public void consumeInventoryUpdated(
      InventoryUpdatedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {
    log.info("Received inventory-updated event: {}", event);
    orderService.markInventoryConfirmed(
        envelope(event, event.getEventId(), event.getOrderId(), topic, partition, offset));
  }

  @KafkaListener(
      topics = "${app.kafka.topics.inventory-failed:inventory-failed}",
      groupId = CONSUMER_GROUP)
  public void consumeInventoryFailed(
      InventoryFailedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {
    log.info("Received inventory-failed event: {}", event);
    orderService.markInventoryFailed(
        envelope(event, event.getEventId(), event.getOrderId(), topic, partition, offset),
        event.getReason());
  }

  @KafkaListener(
      topics = "${app.kafka.topics.payment-refunded:payment-refunded}",
      groupId = CONSUMER_GROUP)
  public void consumePaymentRefunded(
      PaymentRefundedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {
    log.info("Received payment-refunded event: {}", event);
    orderService.markRefunded(
        envelope(event, event.getEventId(), event.getOrderId(), topic, partition, offset),
        event.getReason());
  }

  private OrderEventEnvelope envelope(
      SpecificRecordBase event,
      String eventId,
      String orderId,
      String topic,
      int partition,
      long offset) {
    return new OrderEventEnvelope(
        eventId,
        orderId,
        event.getClass().getSimpleName(),
        topic + "[" + partition + "]@" + offset,
        payloadSerializer.toJson(event));
  }
}
