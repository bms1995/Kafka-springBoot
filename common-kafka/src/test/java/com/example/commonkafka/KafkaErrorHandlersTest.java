package com.example.commonkafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KafkaErrorHandlersTest {

  @Test
  void exposesStandardRetryDefaults() {
    assertThat(KafkaErrorHandlers.DEFAULT_RETRY_INTERVAL_MS).isEqualTo(2000L);
    assertThat(KafkaErrorHandlers.DEFAULT_MAX_RETRY_ATTEMPTS).isEqualTo(3L);
  }
}
