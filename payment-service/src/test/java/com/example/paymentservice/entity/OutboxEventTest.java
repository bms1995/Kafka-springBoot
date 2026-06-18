package com.example.paymentservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutboxEventTest {

  @Test
  void marksEventDeadWhenPublishFailuresReachMaxAttempts() {
    OutboxEvent event = new OutboxEvent("order-1", "payment-processed", "EventType", "{}");

    event.markPublishFailed("broker unavailable", 2, 1000L, 60000L);

    assertThat(event.getAttemptCount()).isEqualTo(1);
    assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(event.getLastError()).isEqualTo("broker unavailable");
    assertThat(event.getNextAttemptAt()).isAfter(event.getCreatedAt());
    assertThat(event.getProcessingStartedAt()).isNull();

    event.markPublishFailed("broker unavailable", 2, 1000L, 60000L);

    assertThat(event.getAttemptCount()).isEqualTo(2);
    assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
    assertThat(event.getNextAttemptAt()).isNull();
    assertThat(event.getProcessingStartedAt()).isNull();
  }

  @Test
  void clearsLastErrorWhenPublished() {
    OutboxEvent event = new OutboxEvent("order-1", "payment-processed", "EventType", "{}");

    event.markPublishFailed("temporary failure", 2, 1000L, 60000L);
    event.markProcessing();
    event.markPublished();

    assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    assertThat(event.getPublishedAt()).isNotNull();
    assertThat(event.getProcessingStartedAt()).isNull();
    assertThat(event.getLastError()).isNull();
  }

  @Test
  void capsRetryBackoffAtConfiguredMaximum() {
    OutboxEvent event = new OutboxEvent("order-1", "payment-processed", "EventType", "{}");

    event.markPublishFailed("temporary failure", 5, 1000L, 1500L);

    assertThat(event.getNextAttemptAt()).isNotNull();
    var firstNextAttemptAt = event.getNextAttemptAt();

    event.markPublishFailed("temporary failure", 5, 1000L, 1500L);

    assertThat(event.getNextAttemptAt()).isAfter(firstNextAttemptAt);
    assertThat(event.getNextAttemptAt())
        .isBeforeOrEqualTo(java.time.Instant.now().plusMillis(1500L));
  }

  @Test
  void failedProcessingEventReturnsToPendingWhenRetryable() {
    OutboxEvent event = new OutboxEvent("order-1", "payment-processed", "EventType", "{}");

    event.markProcessing();

    assertThat(event.getProcessingStartedAt()).isNotNull();

    event.markPublishFailed("temporary failure", 3, 1000L, 60000L);

    assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(event.getNextAttemptAt()).isNotNull();
    assertThat(event.getProcessingStartedAt()).isNull();
  }
}
