package com.example.orderservice.service;

import com.example.events.OrderCreatedEvent;
import com.example.orderservice.api.CreateOrderRequest;
import com.example.orderservice.api.OrderEventResponse;
import com.example.orderservice.api.OrderResponse;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderEventHistory;
import com.example.orderservice.entity.ProcessedEvent;
import com.example.orderservice.event.EventMetadata;
import com.example.orderservice.repository.OrderEventHistoryRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ProcessedEventRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderOutboxService orderOutboxService;
  private final ProcessedEventRepository processedEventRepository;
  private final OrderEventHistoryRepository orderEventHistoryRepository;

  @Transactional
  public String createOrder(CreateOrderRequest request) {
    String orderId =
        (request.orderId() != null && !request.orderId().isBlank())
            ? request.orderId()
            : UUID.randomUUID().toString();

    EventMetadata metadata = EventMetadata.start("order-service");
    OrderCreatedEvent event =
        new OrderCreatedEvent(
            orderId,
            request.productName(),
            request.quantity(),
            request.amount().toPlainString(),
            request.customerEmail(),
            metadata.eventId(),
            metadata.correlationId(),
            metadata.causationId(),
            metadata.occurredAt(),
            metadata.producer(),
            metadata.schemaVersion());

    orderRepository.save(
        new OrderEntity(
            orderId,
            request.productName(),
            request.quantity(),
            request.amount(),
            request.customerEmail()));
    orderOutboxService.enqueue(orderId, event);

    return orderId;
  }

  @Transactional(readOnly = true)
  public OrderResponse getOrder(String orderId) {
    return orderRepository
        .findById(orderId)
        .map(OrderResponse::from)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
  }

  @Transactional
  public void markPaymentConfirmed(OrderEventEnvelope event) {
    processEvent(event, order -> order.markPaymentConfirmed());
  }

  @Transactional
  public void markPaymentFailed(OrderEventEnvelope event, String reason) {
    processEvent(event, order -> order.markPaymentFailed(reason));
  }

  @Transactional
  public void markInventoryConfirmed(OrderEventEnvelope event) {
    processEvent(event, order -> order.markInventoryConfirmed());
  }

  @Transactional
  public void markInventoryFailed(OrderEventEnvelope event, String reason) {
    processEvent(event, order -> order.markInventoryFailed(reason));
  }

  @Transactional
  public void markRefunded(OrderEventEnvelope event, String reason) {
    processEvent(event, order -> order.markRefunded(reason));
  }

  @Transactional(readOnly = true)
  public List<OrderEventResponse> getOrderEvents(String orderId) {
    if (!orderRepository.existsById(orderId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId);
    }

    return orderEventHistoryRepository.findByOrderIdOrderByReceivedAtAsc(orderId).stream()
        .map(OrderEventResponse::from)
        .toList();
  }

  private void processEvent(
      OrderEventEnvelope event, java.util.function.Consumer<OrderEntity> transition) {
    if (processedEventRepository.existsById(event.eventId())) {
      log.info(
          "Skipping duplicate order saga event eventId={} orderId={} type={}",
          event.eventId(),
          event.orderId(),
          event.eventType());
      return;
    }

    OrderEntity order = findOrder(event.orderId());
    transition.accept(order);
    orderEventHistoryRepository.save(
        new OrderEventHistory(
            event.eventId(),
            event.orderId(),
            event.eventType(),
            event.sourceTopic(),
            event.payload()));
    processedEventRepository.save(
        new ProcessedEvent(event.eventId(), event.orderId(), event.eventType()));
  }

  private OrderEntity findOrder(String orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
  }
}
