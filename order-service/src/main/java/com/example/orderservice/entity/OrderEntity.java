package com.example.orderservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "orders")
public class OrderEntity {

    @Id
    private String orderId;

    private String productName;

    private int quantity;

    private BigDecimal amount;

    private String customerEmail;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;

    public OrderEntity(String orderId, String productName, int quantity, BigDecimal amount, String customerEmail) {
        this.orderId = orderId;
        this.productName = productName;
        this.quantity = quantity;
        this.amount = amount;
        this.customerEmail = customerEmail;
        this.status = OrderStatus.CREATED;
        this.createdAt = Instant.now();
    }
}
