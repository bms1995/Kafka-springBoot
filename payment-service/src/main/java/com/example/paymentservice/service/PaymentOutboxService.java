package com.example.paymentservice.service;

import com.example.paymentservice.entity.OutboxEvent;
import com.example.paymentservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PaymentOutboxService {

    public static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    public static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    public static final String PAYMENT_REFUNDED_TOPIC = "payment-refunded";

    private final OutboxEventRepository outboxEventRepository;

    public void enqueue(String topic, String aggregateId, Object event) {
        try {
            SpecificRecordBase avroEvent = (SpecificRecordBase) event;
            outboxEventRepository.save(new OutboxEvent(
                    aggregateId,
                    topic,
                    event.getClass().getName(),
                    toAvroJson(avroEvent)
            ));
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
