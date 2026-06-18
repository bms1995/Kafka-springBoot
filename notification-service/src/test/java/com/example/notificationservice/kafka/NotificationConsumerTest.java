package com.example.notificationservice.kafka;

import static org.assertj.core.api.Assertions.assertThatNoException;

import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentRefundedEvent;
import org.junit.jupiter.api.Test;

class NotificationConsumerTest {

  private final NotificationConsumer consumer = new NotificationConsumer();

  @Test
  void consumeInventoryUpdatedEventDoesNotThrow() {
    InventoryUpdatedEvent event =
        new InventoryUpdatedEvent(
            "order-1",
            "UPDATED",
            "inventory-updated-event-1",
            "correlation-1",
            "payment-processed-event-1",
            "2026-06-12T10:00:00Z",
            "inventory-service",
            "1");

    assertThatNoException().isThrownBy(() -> consumer.consume(event));
  }

  @Test
  void consumePaymentFailureEventDoesNotThrow() {
    PaymentFailedEvent event =
        new PaymentFailedEvent(
            "order-1",
            "FAILED",
            "Payment declined",
            "customer@example.com",
            "payment-failed-event-1",
            "correlation-1",
            "order-created-event-1",
            "2026-06-12T10:00:00Z",
            "payment-service",
            "1");

    assertThatNoException().isThrownBy(() -> consumer.consumePaymentFailure(event));
  }

  @Test
  void consumePaymentRefundEventDoesNotThrow() {
    PaymentRefundedEvent event =
        new PaymentRefundedEvent(
            "order-1",
            "REFUNDED",
            "Inventory unavailable",
            "payment-refunded-event-1",
            "correlation-1",
            "inventory-failed-event-1",
            "2026-06-12T10:00:00Z",
            "payment-service",
            "1");

    assertThatNoException().isThrownBy(() -> consumer.consumePaymentRefund(event));
  }
}
