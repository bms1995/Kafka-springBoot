package com.example.orderservice.metrics;

import com.example.orderservice.entity.OutboxStatus;
import com.example.orderservice.repository.OutboxEventRepository;
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
        Gauge.builder("order_outbox_pending_events", outboxEventRepository,
                        repository -> repository.countByStatus(OutboxStatus.PENDING))
                .description("Number of pending order outbox events")
                .register(registry);

        Gauge.builder("order_outbox_dead_events", outboxEventRepository,
                        repository -> repository.countByStatus(OutboxStatus.DEAD))
                .description("Number of dead order outbox events")
                .register(registry);
    }
}
