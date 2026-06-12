package com.example.paymentservice.health;

import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxHealthIndicator implements HealthIndicator {

    private static final long PENDING_EVENTS_WARNING_THRESHOLD = 100;

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public Health health() {
        long pendingEvents = outboxEventRepository.countByStatus(OutboxStatus.PENDING);

        if (pendingEvents > PENDING_EVENTS_WARNING_THRESHOLD) {
            return Health.down()
                    .withDetail("pendingEvents", pendingEvents)
                    .withDetail("threshold", PENDING_EVENTS_WARNING_THRESHOLD)
                    .build();
        }

        return Health.up()
                .withDetail("pendingEvents", pendingEvents)
                .build();
    }
}
