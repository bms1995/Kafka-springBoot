package com.example.inventoryservice.kafka;

import com.example.events.InventoryFailedEvent;
import com.example.events.InventoryUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.inventory-updated:inventory-updated}")
    private String inventoryUpdatedTopic;

    @Value("${app.kafka.topics.inventory-failed:inventory-failed}")
    private String inventoryFailedTopic;

    public void send(InventoryUpdatedEvent event) {
        kafkaTemplate.send(inventoryUpdatedTopic, event.getOrderId(), event);
        log.info("Published inventory-updated event: {}", event);
    }

    public void sendFailure(InventoryFailedEvent event) {
        kafkaTemplate.send(inventoryFailedTopic, event.getOrderId(), event);
        log.info("Published inventory-failed event: {}", event);
    }
}
