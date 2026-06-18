package com.example.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "processed_events")
public class ProcessedEvent {

  @Id
  @Column(name = "event_id")
  private String eventId;

  @Column(name = "order_id", nullable = false)
  private String orderId;

  public ProcessedEvent(String eventId, String orderId) {
    this.eventId = eventId;
    this.orderId = orderId;
  }
}
