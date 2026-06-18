package com.example.orderservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "processed_events")
public class ProcessedEvent {

  @Id private String eventId;

  private String orderId;

  private String eventType;

  private Instant processedAt;

  public ProcessedEvent(String eventId, String orderId, String eventType) {
    this.eventId = eventId;
    this.orderId = orderId;
    this.eventType = eventType;
    this.processedAt = Instant.now();
  }
}
