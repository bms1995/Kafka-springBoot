package com.example.orderservice.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    void marksEventDeadWhenPublishFailuresReachMaxAttempts() {
        OutboxEvent event = new OutboxEvent("order-1", "order-created", "EventType", "{}");

        event.markPublishFailed("broker unavailable", 2);

        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getLastError()).isEqualTo("broker unavailable");

        event.markPublishFailed("broker unavailable", 2);

        assertThat(event.getAttemptCount()).isEqualTo(2);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
    }

    @Test
    void clearsLastErrorWhenPublished() {
        OutboxEvent event = new OutboxEvent("order-1", "order-created", "EventType", "{}");

        event.markPublishFailed("temporary failure", 2);
        event.markPublished();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
    }
}
