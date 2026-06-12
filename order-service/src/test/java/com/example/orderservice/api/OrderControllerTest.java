package com.example.orderservice.api;

import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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
}
