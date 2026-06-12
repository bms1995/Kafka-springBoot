package com.example.orderservice.repository;

import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    long countByStatus(OutboxStatus status);
}
