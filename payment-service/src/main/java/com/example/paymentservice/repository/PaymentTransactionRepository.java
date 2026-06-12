package com.example.paymentservice.repository;

import com.example.paymentservice.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
}
