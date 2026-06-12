package com.example.orderservice.api;

import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Test
    void createOrderQueuesEventWithProvidedOrderId() {
        OrderController controller = new OrderController(orderService);
        CreateOrderRequest request = new CreateOrderRequest(
                "order-1",
                "Laptop",
                2,
                BigDecimal.valueOf(349.99),
                "customer@example.com"
        );
        when(orderService.createOrder(request)).thenReturn("order-1");

        String response = controller.createOrder(request);

        assertThat(response).isEqualTo("Order created and queued for Kafka with ID: order-1");
    }

    @Test
    void createOrderGeneratesOrderIdWhenMissing() {
        OrderController controller = new OrderController(orderService);
        CreateOrderRequest request = new CreateOrderRequest(
                " ",
                "Keyboard",
                1,
                BigDecimal.valueOf(79.90),
                "customer@example.com"
        );
        when(orderService.createOrder(request)).thenReturn("generated-order-id");

        String response = controller.createOrder(request);

        assertThat(response).isEqualTo(
                "Order created and queued for Kafka with ID: generated-order-id"
        );
    }

    @Test
    void getOrderReturnsOrderReadModel() {
        OrderController controller = new OrderController(orderService);
        OrderResponse orderResponse = new OrderResponse(
                "order-1",
                "Laptop",
                2,
                BigDecimal.valueOf(349.99),
                "customer@example.com",
                OrderStatus.INVENTORY_CONFIRMED,
                null,
                Instant.parse("2026-06-12T10:00:00Z"),
                Instant.parse("2026-06-12T10:01:00Z")
        );
        when(orderService.getOrder("order-1")).thenReturn(orderResponse);

        OrderResponse response = controller.getOrder("order-1");

        assertThat(response).isEqualTo(orderResponse);
    }

    @Test
    void getOrderEventsReturnsTimeline() {
        OrderController controller = new OrderController(orderService);
        OrderEventResponse event = new OrderEventResponse(
                "payment-processed-0-1",
                "order-1",
                "PaymentProcessedEvent",
                "payment-processed",
                "{\"orderId\":\"order-1\"}",
                Instant.parse("2026-06-12T10:01:00Z")
        );
        when(orderService.getOrderEvents("order-1")).thenReturn(List.of(event));

        List<OrderEventResponse> response = controller.getOrderEvents("order-1");

        assertThat(response).containsExactly(event);
    }
}
