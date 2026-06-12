package com.example.inventoryservice.kafka;

import com.example.events.InventoryFailedEvent;
import com.example.events.InventoryUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryProducer {

    private static final String INVENTORY_UPDATED_TOPIC = "inventory-updated";
    private static final String INVENTORY_FAILED_TOPIC = "inventory-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(InventoryUpdatedEvent event) {
        kafkaTemplate.send(INVENTORY_UPDATED_TOPIC, event.getOrderId(), event);
        log.info("Published inventory-updated event: {}", event);
    }

    public void sendFailure(InventoryFailedEvent event) {
        kafkaTemplate.send(INVENTORY_FAILED_TOPIC, event.getOrderId(), event);
        log.info("Published inventory-failed event: {}", event);
    }
}
