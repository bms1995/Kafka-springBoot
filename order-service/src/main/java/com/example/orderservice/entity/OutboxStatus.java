package com.example.orderservice.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    DEAD
}
