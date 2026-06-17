package com.example.paymentservice.kafka;

import com.example.paymentservice.entity.OutboxEvent;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.metrics.PaymentMetrics;
import com.example.paymentservice.repository.OutboxEventRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentMetrics paymentMetrics;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${app.outbox.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent outboxEvent : outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)) {
            publish(outboxEvent);
        }
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            Class<?> eventClass = Class.forName(outboxEvent.getEventType());
            Object event = fromAvroJson(outboxEvent, eventClass);

            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getAggregateId(), event);
            outboxEvent.markPublished();
            outboxEventRepository.save(outboxEvent);
            paymentMetrics.incrementOutboxPublished();

            log.info("Published outbox event eventId={} topic={} aggregateId={}",
                    outboxEvent.getEventId(),
                    outboxEvent.getTopic(),
                    outboxEvent.getAggregateId());
        } catch (Exception ex) {
            outboxEvent.markPublishFailed(ex.getMessage(), maxAttempts);
            outboxEventRepository.save(outboxEvent);
            paymentMetrics.incrementOutboxPublishFailed();
            log.error("Could not publish outbox event eventId={} attemptCount={} status={}",
                    outboxEvent.getEventId(),
                    outboxEvent.getAttemptCount(),
                    outboxEvent.getStatus(),
                    ex);
        }
    }

    private Object fromAvroJson(OutboxEvent outboxEvent, Class<?> eventClass) throws Exception {
        Schema schema = (Schema) eventClass.getField("SCHEMA$").get(null);
        SpecificDatumReader<SpecificRecordBase> reader = new SpecificDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, outboxEvent.getPayload());
        return reader.read(null, decoder);
    }
}
