package com.example.commonevents;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EventTopicsTest {

  @Test
  void buildsDeadLetterTopicName() {
    assertThat(EventTopics.deadLetterTopic(EventTopics.ORDER_CREATED))
        .isEqualTo("order-created.DLQ");
  }

  @Test
  void exposesCanonicalTopicNames() {
    assertThat(EventTopics.PAYMENT_PROCESSED).isEqualTo("payment-processed");
    assertThat(EventTopics.INVENTORY_FAILED).isEqualTo("inventory-failed");
  }
}
