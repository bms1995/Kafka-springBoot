package com.example.orderservice.service;

import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.repository.OutboxEventRepository;
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
public class OrderOutboxService {

    public static final String ORDER_CREATED_TOPIC = "order-created";

    private final OutboxEventRepository outboxEventRepository;

    public void enqueue(String aggregateId, Object event) {
        try {
            SpecificRecordBase avroEvent = (SpecificRecordBase) event;
            outboxEventRepository.save(new OutboxEvent(
                    aggregateId,
                    ORDER_CREATED_TOPIC,
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
