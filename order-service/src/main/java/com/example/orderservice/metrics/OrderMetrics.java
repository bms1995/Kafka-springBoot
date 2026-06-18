package com.example.orderservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMetrics {

  private final MeterRegistry meterRegistry;

  public void incrementOutboxPublished() {
    counter("order_outbox_published_total").increment();
  }

  public void incrementOutboxPublishFailed() {
    counter("order_outbox_publish_failed_total").increment();
  }

  private Counter counter(String name) {
    return Counter.builder(name).register(meterRegistry);
  }
}
