package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderEventHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventHistoryRepository extends JpaRepository<OrderEventHistory, String> {
    List<OrderEventHistory> findByOrderIdOrderByReceivedAtAsc(String orderId);
}
