package com.example.paymentservice.repository;

import com.example.paymentservice.entity.OutboxEvent;
import com.example.paymentservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<OutboxEvent> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxStatus status,
            Instant nextAttemptAt
    );

    @Query(value = """
            select *
            from outbox_events
            where status = 'PENDING'
              and next_attempt_at <= :now
            order by created_at asc
            limit 50
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> claimPublishableEvents(@Param("now") Instant now);

    long countByStatus(OutboxStatus status);
}
