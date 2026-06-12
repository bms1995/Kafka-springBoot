package com.example.orderservice.service;

import com.example.orderservice.api.CreateOrderRequest;
import com.example.orderservice.api.OrderResponse;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderOutboxService orderOutboxService;

    @Test
    void createOrderPersistsOrderAndQueuesEvent() {
        OrderService orderService = new OrderService(orderRepository, orderOutboxService);
        CreateOrderRequest request = new CreateOrderRequest(
                "order-1",
                "Laptop",
                2,
                BigDecimal.valueOf(349.99),
                "customer@example.com"
        );

        String orderId = orderService.createOrder(request);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        verify(orderOutboxService).enqueue(eq("order-1"), any());

        OrderEntity order = orderCaptor.getValue();
        assertThat(orderId).isEqualTo("order-1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getFailureReason()).isNull();
    }

    @Test
    void sagaEventsUpdateOrderReadModel() {
        OrderService orderService = new OrderService(orderRepository, orderOutboxService);
        OrderEntity order = new OrderEntity(
                "order-1",
                "Laptop",
                2,
                BigDecimal.valueOf(349.99),
                "customer@example.com"
        );
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        orderService.markPaymentConfirmed("order-1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);

        orderService.markInventoryFailed("order-1", "OUT_OF_STOCK");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_FAILED);
        assertThat(order.getFailureReason()).isEqualTo("OUT_OF_STOCK");

        orderService.markRefunded("order-1", "Inventory unavailable");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getFailureReason()).isEqualTo("Inventory unavailable");
    }

    @Test
    void getOrderReturnsReadModel() {
        OrderService orderService = new OrderService(orderRepository, orderOutboxService);
        OrderEntity order = new OrderEntity(
                "order-1",
                "Laptop",
                2,
                BigDecimal.valueOf(349.99),
                "customer@example.com"
        );
        order.markInventoryConfirmed();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder("order-1");

        assertThat(response.orderId()).isEqualTo("order-1");
        assertThat(response.status()).isEqualTo(OrderStatus.INVENTORY_CONFIRMED);
    }
}
