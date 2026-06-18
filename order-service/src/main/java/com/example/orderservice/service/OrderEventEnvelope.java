package com.example.orderservice.service;

public record OrderEventEnvelope(
    String eventId, String orderId, String eventType, String sourceTopic, String payload) {}
