package com.example.orderservice.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.orderservice.entity.OutboxStatus;
import com.example.orderservice.repository.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class OutboxMetricsTest {

  @Test
  void exposesPendingAndDeadOutboxGauges() {
    OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    when(outboxEventRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(7L);
    when(outboxEventRepository.countByStatus(OutboxStatus.DEAD)).thenReturn(2L);

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    new OutboxMetrics(outboxEventRepository).bindTo(registry);

    assertThat(registry.get("order_outbox_pending_events").gauge().value()).isEqualTo(7.0);
    assertThat(registry.get("order_outbox_dead_events").gauge().value()).isEqualTo(2.0);
  }
}
