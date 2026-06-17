package com.example.paymentservice.health;

import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxHealthIndicatorTest {

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final OutboxHealthIndicator healthIndicator = new OutboxHealthIndicator(outboxEventRepository);

    @Test
    void reportsUpWhenOutboxHasNoDeadEventsAndPendingBacklogIsLow() {
        when(outboxEventRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(3L);
        when(outboxEventRepository.countByStatus(OutboxStatus.DEAD)).thenReturn(0L);

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("pendingEvents", 3L);
        assertThat(health.getDetails()).containsEntry("deadEvents", 0L);
    }

    @Test
    void reportsDownWhenOutboxHasDeadEvents() {
        when(outboxEventRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(0L);
        when(outboxEventRepository.countByStatus(OutboxStatus.DEAD)).thenReturn(1L);

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("deadEvents", 1L);
    }

    @Test
    void reportsDownWhenPendingBacklogExceedsThreshold() {
        when(outboxEventRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(101L);
        when(outboxEventRepository.countByStatus(OutboxStatus.DEAD)).thenReturn(0L);

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("pendingEvents", 101L);
    }
}
