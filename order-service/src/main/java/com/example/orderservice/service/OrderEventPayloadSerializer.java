package com.example.orderservice.service;

import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Component
public class OrderEventPayloadSerializer {

    public String toJson(SpecificRecordBase event) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            SpecificDatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(event.getSchema());
            Encoder encoder = EncoderFactory.get().jsonEncoder(event.getSchema(), outputStream);
            writer.write(event, encoder);
            encoder.flush();
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize order event payload", ex);
        }
    }
}
