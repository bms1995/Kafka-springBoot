package com.example.commonkafka;

import com.example.commonevents.EventTopics;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

public final class KafkaErrorHandlers {

  public static final long DEFAULT_RETRY_INTERVAL_MS = 2000L;
  public static final long DEFAULT_MAX_RETRY_ATTEMPTS = 3L;

  private KafkaErrorHandlers() {}

  public static DefaultErrorHandler deadLetterErrorHandler(
      KafkaTemplate<Object, Object> kafkaTemplate, Logger log) {
    return deadLetterErrorHandler(
        kafkaTemplate, log, DEFAULT_RETRY_INTERVAL_MS, DEFAULT_MAX_RETRY_ATTEMPTS);
  }

  public static DefaultErrorHandler deadLetterErrorHandler(
      KafkaTemplate<Object, Object> kafkaTemplate,
      Logger log,
      long retryIntervalMs,
      long maxRetryAttempts) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) ->
                new TopicPartition(
                    EventTopics.deadLetterTopic(record.topic()), record.partition()));

    DefaultErrorHandler handler =
        new DefaultErrorHandler(recoverer, new FixedBackOff(retryIntervalMs, maxRetryAttempts));

    handler.setRetryListeners(
        (record, ex, deliveryAttempt) ->
            log.warn(
                "Retry attempt {} for topic={}, key={}, value={}",
                deliveryAttempt,
                record.topic(),
                record.key(),
                record.value(),
                ex));

    return handler;
  }
}
