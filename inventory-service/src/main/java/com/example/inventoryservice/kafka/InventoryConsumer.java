package com.example.inventoryservice.kafka;

import com.example.events.PaymentProcessedEvent;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "payment-processed", groupId = "inventory-group")
    public void consume(PaymentProcessedEvent event) {
        log.info("Received payment-processed event: {}", event);
        inventoryService.updateInventory(event);
    }
}
