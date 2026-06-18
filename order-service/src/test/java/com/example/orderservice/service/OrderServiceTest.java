package com.example.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.orderservice.api.CreateOrderRequest;
import com.example.orderservice.api.OrderEventResponse;
import com.example.orderservice.api.OrderResponse;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderEventHistory;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderEventHistoryRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ProcessedEventRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private OrderOutboxService orderOutboxService;

  @Mock private ProcessedEventRepository processedEventRepository;

  @Mock private OrderEventHistoryRepository orderEventHistoryRepository;

  @Test
  void createOrderPersistsOrderAndQueuesEvent() {
    OrderService orderService =
        new OrderService(
            orderRepository,
            orderOutboxService,
            processedEventRepository,
            orderEventHistoryRepository);
    CreateOrderRequest request =
        new CreateOrderRequest(
            "order-1", "Laptop", 2, BigDecimal.valueOf(349.99), "customer@example.com");

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
    OrderService orderService =
        new OrderService(
            orderRepository,
            orderOutboxService,
            processedEventRepository,
            orderEventHistoryRepository);
    OrderEntity order =
        new OrderEntity("order-1", "Laptop", 2, BigDecimal.valueOf(349.99), "customer@example.com");
    when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

    orderService.markPaymentConfirmed(event("payment-processed-0-1", "PaymentProcessedEvent"));
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);

    orderService.markInventoryFailed(
        event("inventory-failed-0-2", "InventoryFailedEvent"), "OUT_OF_STOCK");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_FAILED);
    assertThat(order.getFailureReason()).isEqualTo("OUT_OF_STOCK");

    orderService.markRefunded(
        event("payment-refunded-0-3", "PaymentRefundedEvent"), "Inventory unavailable");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
    assertThat(order.getFailureReason()).isEqualTo("Inventory unavailable");
    verify(orderEventHistoryRepository, org.mockito.Mockito.times(3)).save(any());
    verify(processedEventRepository, org.mockito.Mockito.times(3)).save(any());
  }

  @Test
  void getOrderReturnsReadModel() {
    OrderService orderService =
        new OrderService(
            orderRepository,
            orderOutboxService,
            processedEventRepository,
            orderEventHistoryRepository);
    OrderEntity order =
        new OrderEntity("order-1", "Laptop", 2, BigDecimal.valueOf(349.99), "customer@example.com");
    order.markInventoryConfirmed();
    when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

    OrderResponse response = orderService.getOrder("order-1");

    assertThat(response.orderId()).isEqualTo("order-1");
    assertThat(response.status()).isEqualTo(OrderStatus.INVENTORY_CONFIRMED);
  }

  @Test
  void duplicateSagaEventIsSkipped() {
    OrderService orderService =
        new OrderService(
            orderRepository,
            orderOutboxService,
            processedEventRepository,
            orderEventHistoryRepository);
    when(processedEventRepository.existsById("payment-processed-0-1")).thenReturn(true);

    orderService.markPaymentConfirmed(event("payment-processed-0-1", "PaymentProcessedEvent"));

    verify(orderRepository, never()).findById("order-1");
    verify(orderEventHistoryRepository, never()).save(any());
  }

  @Test
  void getOrderEventsReturnsTimeline() {
    OrderService orderService =
        new OrderService(
            orderRepository,
            orderOutboxService,
            processedEventRepository,
            orderEventHistoryRepository);
    OrderEventHistory history =
        new OrderEventHistory(
            "payment-processed-0-1",
            "order-1",
            "PaymentProcessedEvent",
            "payment-processed",
            "{\"orderId\":\"order-1\"}");
    when(orderRepository.existsById("order-1")).thenReturn(true);
    when(orderEventHistoryRepository.findByOrderIdOrderByReceivedAtAsc("order-1"))
        .thenReturn(List.of(history));

    List<OrderEventResponse> events = orderService.getOrderEvents("order-1");

    assertThat(events).hasSize(1);
    assertThat(events.getFirst().eventId()).isEqualTo("payment-processed-0-1");
    assertThat(events.getFirst().eventType()).isEqualTo("PaymentProcessedEvent");
  }

  private OrderEventEnvelope event(String eventId, String eventType) {
    return new OrderEventEnvelope(
        eventId,
        "order-1",
        eventType,
        eventType.replace("Event", "").toLowerCase(),
        "{\"orderId\":\"order-1\"}");
  }
}
