package com.example.commonkafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.FailedRecordProcessor;
import org.springframework.kafka.listener.RetryListener;

class KafkaErrorHandlersTest {

  @Test
  void exposesStandardRetryDefaults() {
    assertThat(KafkaErrorHandlers.DEFAULT_RETRY_INTERVAL_MS).isEqualTo(2000L);
    assertThat(KafkaErrorHandlers.DEFAULT_MAX_RETRY_ATTEMPTS).isEqualTo(3L);
  }

  @Test
  void deadLetterErrorHandlerLogsRetryAttemptsWithTheExceptionMessage() throws Exception {
    Logger log = mock(Logger.class);
    KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
    DefaultErrorHandler handler =
        KafkaErrorHandlers.deadLetterErrorHandler(kafkaTemplate, log, 1500L, 2L);

    List<RetryListener> retryListeners = retryListeners(handler);
    assertThat(retryListeners).hasSize(1);

    ConsumerRecord<Object, Object> record = new ConsumerRecord<>("orders", 0, 42L, "order-1", "v1");
    retryListeners.get(0).failedDelivery(record, new IllegalStateException("boom"), 2);

    verify(log)
        .warn(
            "Retry attempt {} for topic={}, key={}, value={}, cause={}",
            2,
            "orders",
            "order-1",
            "v1",
            "boom");
  }

  @SuppressWarnings("unchecked")
  private static List<RetryListener> retryListeners(DefaultErrorHandler handler) throws Exception {
    Method method = FailedRecordProcessor.class.getDeclaredMethod("getRetryListeners");
    method.setAccessible(true);
    return (List<RetryListener>) method.invoke(handler);
  }
}
