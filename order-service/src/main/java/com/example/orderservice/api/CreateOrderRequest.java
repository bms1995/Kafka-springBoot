package com.example.orderservice.api;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String orderId,
        String productName,
        Integer quantity,
        BigDecimal amount,
        String customerEmail
) {
}
