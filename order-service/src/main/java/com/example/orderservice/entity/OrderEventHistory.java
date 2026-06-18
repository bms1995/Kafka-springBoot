package com.example.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "order_event_history")
public class OrderEventHistory {

  @Id private String historyId;

  private String eventId;

  private String orderId;

  private String eventType;

  private String sourceTopic;

  @Column(columnDefinition = "text")
  private String payload;

  private Instant receivedAt;

  public OrderEventHistory(
      String eventId, String orderId, String eventType, String sourceTopic, String payload) {
    this.historyId = UUID.randomUUID().toString();
    this.eventId = eventId;
    this.orderId = orderId;
    this.eventType = eventType;
    this.sourceTopic = sourceTopic;
    this.payload = payload;
    this.receivedAt = Instant.now();
  }
}
