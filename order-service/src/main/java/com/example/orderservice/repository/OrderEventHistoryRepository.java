package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderEventHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventHistoryRepository extends JpaRepository<OrderEventHistory, String> {
  List<OrderEventHistory> findByOrderIdOrderByReceivedAtAsc(String orderId);
}
