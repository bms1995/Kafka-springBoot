package com.example.orderservice.entity;

public enum OrderStatus {
    CREATED,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    INVENTORY_CONFIRMED,
    INVENTORY_FAILED,
    REFUNDED
}
