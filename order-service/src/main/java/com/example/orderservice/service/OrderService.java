package com.example.orderservice.service;

import com.example.events.OrderCreatedEvent;
import com.example.orderservice.api.CreateOrderRequest;
import com.example.orderservice.api.OrderResponse;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderOutboxService orderOutboxService;

    @Transactional
    public String createOrder(CreateOrderRequest request) {
        String orderId = (request.orderId() != null && !request.orderId().isBlank())
                ? request.orderId()
                : UUID.randomUUID().toString();

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                request.productName(),
                request.quantity(),
                request.amount().toPlainString(),
                request.customerEmail()
        );

        orderRepository.save(new OrderEntity(
                orderId,
                request.productName(),
                request.quantity(),
                request.amount(),
                request.customerEmail()
        ));
        orderOutboxService.enqueue(orderId, event);

        return orderId;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
    }

    @Transactional
    public void markPaymentConfirmed(String orderId) {
        findOrder(orderId).markPaymentConfirmed();
    }

    @Transactional
    public void markPaymentFailed(String orderId, String reason) {
        findOrder(orderId).markPaymentFailed(reason);
    }

    @Transactional
    public void markInventoryConfirmed(String orderId) {
        findOrder(orderId).markInventoryConfirmed();
    }

    @Transactional
    public void markInventoryFailed(String orderId, String reason) {
        findOrder(orderId).markInventoryFailed(reason);
    }

    @Transactional
    public void markRefunded(String orderId, String reason) {
        findOrder(orderId).markRefunded(reason);
    }

    private OrderEntity findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
    }
}
