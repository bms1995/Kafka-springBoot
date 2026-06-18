package com.example.orderservice.api;

import com.example.orderservice.entity.OrderEventHistory;
import java.time.Instant;

public record OrderEventResponse(
    String eventId,
    String orderId,
    String eventType,
    String sourceTopic,
    String payload,
    Instant receivedAt) {
  public static OrderEventResponse from(OrderEventHistory history) {
    return new OrderEventResponse(
        history.getEventId(),
        history.getOrderId(),
        history.getEventType(),
        history.getSourceTopic(),
        history.getPayload(),
        history.getReceivedAt());
  }
}
