package com.example.paymentservice.service;

import com.example.commonevents.EventTopics;
import com.example.paymentservice.entity.OutboxEvent;
import com.example.paymentservice.repository.OutboxEventRepository;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentOutboxService {

  public static final String PAYMENT_PROCESSED_TOPIC = EventTopics.PAYMENT_PROCESSED;
  public static final String PAYMENT_FAILED_TOPIC = EventTopics.PAYMENT_FAILED;
  public static final String PAYMENT_REFUNDED_TOPIC = EventTopics.PAYMENT_REFUNDED;

  private final OutboxEventRepository outboxEventRepository;

  @Value("${app.kafka.topics.payment-processed:payment-processed}")
  private String paymentProcessedTopic;

  @Value("${app.kafka.topics.payment-failed:payment-failed}")
  private String paymentFailedTopic;

  @Value("${app.kafka.topics.payment-refunded:payment-refunded}")
  private String paymentRefundedTopic;

  public void enqueuePaymentProcessed(String aggregateId, Object event) {
    enqueue(paymentProcessedTopic, aggregateId, event);
  }

  public void enqueuePaymentFailed(String aggregateId, Object event) {
    enqueue(paymentFailedTopic, aggregateId, event);
  }

  public void enqueuePaymentRefunded(String aggregateId, Object event) {
    enqueue(paymentRefundedTopic, aggregateId, event);
  }

  public void enqueue(String topic, String aggregateId, Object event) {
    try {
      SpecificRecordBase avroEvent = (SpecificRecordBase) event;
      outboxEventRepository.save(
          new OutboxEvent(aggregateId, topic, event.getClass().getName(), toAvroJson(avroEvent)));
    } catch (Exception ex) {
      throw new IllegalStateException("Could not serialize outbox event", ex);
    }
  }

  private String toAvroJson(SpecificRecordBase event) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    SpecificDatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(event.getSchema());
    Encoder encoder = EncoderFactory.get().jsonEncoder(event.getSchema(), outputStream);
    writer.write(event, encoder);
    encoder.flush();
    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
