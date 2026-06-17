package com.example.paymentservice.repository;

import com.example.paymentservice.entity.OutboxEvent;
import com.example.paymentservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<OutboxEvent> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxStatus status,
            Instant nextAttemptAt
    );

    long countByStatus(OutboxStatus status);
}
