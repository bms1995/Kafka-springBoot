package com.example.paymentservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMetrics {

  private final MeterRegistry meterRegistry;

  public void incrementPaymentSucceeded() {
    counter("payment_succeeded_total").increment();
  }

  public void incrementPaymentFailed() {
    counter("payment_failed_total").increment();
  }

  public void incrementDuplicatePaymentSkipped() {
    counter("payment_duplicate_skipped_total").increment();
  }

  public void incrementPaymentRefunded() {
    counter("payment_refunded_total").increment();
  }

  public void incrementOutboxPublished() {
    counter("payment_outbox_published_total").increment();
  }

  public void incrementOutboxPublishFailed() {
    counter("payment_outbox_publish_failed_total").increment();
  }

  private Counter counter(String name) {
    return Counter.builder(name).register(meterRegistry);
  }
}
