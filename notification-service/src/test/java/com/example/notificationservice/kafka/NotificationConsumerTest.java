package com.example.notificationservice.kafka;

import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentRefundedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class NotificationConsumerTest {

    private final NotificationConsumer consumer = new NotificationConsumer();

    @Test
    void consumeInventoryUpdatedEventDoesNotThrow() {
        InventoryUpdatedEvent event = new InventoryUpdatedEvent("order-1", "UPDATED");

        assertThatNoException().isThrownBy(() -> consumer.consume(event));
    }

    @Test
    void consumePaymentFailureEventDoesNotThrow() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                "order-1",
                "FAILED",
                "Payment declined",
                "customer@example.com"
        );

        assertThatNoException().isThrownBy(() -> consumer.consumePaymentFailure(event));
    }

    @Test
    void consumePaymentRefundEventDoesNotThrow() {
        PaymentRefundedEvent event = new PaymentRefundedEvent(
                "order-1",
                "REFUNDED",
                "Inventory unavailable"
        );

        assertThatNoException().isThrownBy(() -> consumer.consumePaymentRefund(event));
    }
}
