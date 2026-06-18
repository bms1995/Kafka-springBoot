package com.example.orderservice.kafka;

import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.metrics.OrderMetrics;
import com.example.orderservice.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOutboxPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final OrderMetrics orderMetrics;
  private final OutboxEventRepository outboxEventRepository;
  private final TransactionTemplate transactionTemplate;

  @Value("${app.outbox.max-attempts:5}")
  private int maxAttempts;

  @Value("${app.outbox.base-backoff-ms:1000}")
  private long baseBackoffMs;

  @Value("${app.outbox.max-backoff-ms:60000}")
  private long maxBackoffMs;

  @Value("${app.outbox.processing-timeout-ms:120000}")
  private long processingTimeoutMs;

  @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
  public void publishPendingEvents() {
    for (OutboxEvent outboxEvent : claimPublishableEvents()) {
      publish(outboxEvent);
    }
  }

  private List<OutboxEvent> claimPublishableEvents() {
    return transactionTemplate.execute(
        status -> {
          releaseStaleProcessingEvents();
          List<OutboxEvent> outboxEvents =
              outboxEventRepository.claimPublishableEvents(Instant.now());
          outboxEvents.forEach(OutboxEvent::markProcessing);
          return outboxEventRepository.saveAll(outboxEvents);
        });
  }

  private void publish(OutboxEvent outboxEvent) {
    try {
      Class<?> eventClass = Class.forName(outboxEvent.getEventType());
      Object event = fromAvroJson(outboxEvent, eventClass);

      kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getAggregateId(), event).get();
      transactionTemplate.executeWithoutResult(
          status -> {
            outboxEvent.markPublished();
            outboxEventRepository.save(outboxEvent);
          });
      orderMetrics.incrementOutboxPublished();

      log.info(
          "Published order outbox event eventId={} topic={} aggregateId={}",
          outboxEvent.getEventId(),
          outboxEvent.getTopic(),
          outboxEvent.getAggregateId());
    } catch (Exception ex) {
      transactionTemplate.executeWithoutResult(
          status -> {
            outboxEvent.markPublishFailed(
                ex.getMessage(), maxAttempts, baseBackoffMs, maxBackoffMs);
            outboxEventRepository.save(outboxEvent);
          });
      orderMetrics.incrementOutboxPublishFailed();
      log.error(
          "Could not publish order outbox event eventId={} attemptCount={} status={}",
          outboxEvent.getEventId(),
          outboxEvent.getAttemptCount(),
          outboxEvent.getStatus(),
          ex);
    }
  }

  private void releaseStaleProcessingEvents() {
    Instant now = Instant.now();
    int released =
        outboxEventRepository.releaseStaleProcessingEvents(
            now.minusMillis(processingTimeoutMs), now);
    if (released > 0) {
      log.warn("Released stale order outbox events count={}", released);
    }
  }

  private Object fromAvroJson(OutboxEvent outboxEvent, Class<?> eventClass) throws Exception {
    Schema schema = (Schema) eventClass.getField("SCHEMA$").get(null);
    SpecificDatumReader<SpecificRecordBase> reader = new SpecificDatumReader<>(schema);
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, outboxEvent.getPayload());
    return reader.read(null, decoder);
  }
}
