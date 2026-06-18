package com.example.commonevents;

public final class EventTopics {

  public static final String ORDER_CREATED = "order-created";
  public static final String PAYMENT_PROCESSED = "payment-processed";
  public static final String PAYMENT_FAILED = "payment-failed";
  public static final String PAYMENT_REFUNDED = "payment-refunded";
  public static final String INVENTORY_UPDATED = "inventory-updated";
  public static final String INVENTORY_FAILED = "inventory-failed";

  private EventTopics() {}

  public static String deadLetterTopic(String topic) {
    return topic + ".DLQ";
  }
}
