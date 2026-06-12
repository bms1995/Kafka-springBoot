package com.example.orderservice.api;

import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String orderId,
        String productName,
        int quantity,
        BigDecimal amount,
        String customerEmail,
        OrderStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getProductName(),
                order.getQuantity(),
                order.getAmount(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getFailureReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
