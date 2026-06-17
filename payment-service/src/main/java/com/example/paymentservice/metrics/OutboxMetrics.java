package com.example.paymentservice.metrics;

import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMetrics implements MeterBinder {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("payment_outbox_pending_events", outboxEventRepository,
                        repository -> repository.countByStatus(OutboxStatus.PENDING))
                .description("Number of pending payment outbox events")
                .register(registry);

        Gauge.builder("payment_outbox_dead_events", outboxEventRepository,
                        repository -> repository.countByStatus(OutboxStatus.DEAD))
                .description("Number of dead payment outbox events")
                .register(registry);
    }
}
