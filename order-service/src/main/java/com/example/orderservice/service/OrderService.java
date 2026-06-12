package com.example.orderservice.service;

import com.example.events.OrderCreatedEvent;
import com.example.orderservice.api.CreateOrderRequest;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
