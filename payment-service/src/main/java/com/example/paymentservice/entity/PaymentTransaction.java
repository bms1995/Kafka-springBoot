package com.example.paymentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    private String orderId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String customerEmail;

    private String failureReason;

    private Instant createdAt;

    private Instant updatedAt;

    public PaymentTransaction(String orderId, BigDecimal amount, String customerEmail) {
        this.orderId = orderId;
        this.amount = amount;
        this.customerEmail = customerEmail;
        this.status = PaymentStatus.PROCESSING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markRefunded(String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }
}
