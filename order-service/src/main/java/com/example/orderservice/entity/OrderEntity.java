package com.example.orderservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "orders")
public class OrderEntity {

  @Id private String orderId;

  private String productName;

  private int quantity;

  private BigDecimal amount;

  private String customerEmail;

  @Enumerated(EnumType.STRING)
  private OrderStatus status;

  private String failureReason;

  private Instant createdAt;

  private Instant updatedAt;

  public OrderEntity(
      String orderId, String productName, int quantity, BigDecimal amount, String customerEmail) {
    this.orderId = orderId;
    this.productName = productName;
    this.quantity = quantity;
    this.amount = amount;
    this.customerEmail = customerEmail;
    this.status = OrderStatus.CREATED;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void markPaymentConfirmed() {
    updateStatus(OrderStatus.PAYMENT_CONFIRMED, null);
  }

  public void markPaymentFailed(String reason) {
    updateStatus(OrderStatus.PAYMENT_FAILED, reason);
  }

  public void markInventoryConfirmed() {
    updateStatus(OrderStatus.INVENTORY_CONFIRMED, null);
  }

  public void markInventoryFailed(String reason) {
    updateStatus(OrderStatus.INVENTORY_FAILED, reason);
  }

  public void markRefunded(String reason) {
    updateStatus(OrderStatus.REFUNDED, reason);
  }

  private void updateStatus(OrderStatus status, String failureReason) {
    this.status = status;
    this.failureReason = failureReason;
    this.updatedAt = Instant.now();
  }
}
