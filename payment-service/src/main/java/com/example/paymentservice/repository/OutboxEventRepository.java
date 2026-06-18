package com.example.paymentservice.repository;

import com.example.paymentservice.entity.OutboxEvent;
import com.example.paymentservice.entity.OutboxStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
  List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

  List<OutboxEvent> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
      OutboxStatus status, Instant nextAttemptAt);

  @Query(
      value =
          """
            select *
            from outbox_events
            where status = 'PENDING'
              and next_attempt_at <= :now
            order by created_at asc
            limit 50
            for update skip locked
            """,
      nativeQuery = true)
  List<OutboxEvent> claimPublishableEvents(@Param("now") Instant now);

  @Modifying
  @Query(
      value =
          """
            update outbox_events
            set status = 'PENDING',
                next_attempt_at = :now,
                processing_started_at = null
            where status = 'PROCESSING'
              and processing_started_at <= :staleBefore
            """,
      nativeQuery = true)
  int releaseStaleProcessingEvents(
      @Param("staleBefore") Instant staleBefore, @Param("now") Instant now);

  long countByStatus(OutboxStatus status);
}
