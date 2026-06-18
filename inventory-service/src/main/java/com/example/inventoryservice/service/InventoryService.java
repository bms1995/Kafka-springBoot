package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.events.InventoryFailedEvent;
import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.inventoryservice.event.EventMetadata;
import com.example.inventoryservice.kafka.InventoryProducer;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String UPDATED_STATUS = "UPDATED";
    private static final String FAILED_STATUS = "FAILED";
    private static final String SUCCESS_PAYMENT_STATUS = "SUCCESS";
    private static final String INVENTORY_FAILURE_ORDER_PREFIX = "fail-inventory";

    private final InventoryProducer inventoryProducer;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void updateInventory(PaymentProcessedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            log.warn("Already processed inventory event eventId={} orderId={}", event.getEventId(), event.getOrderId());
            return;
        }

        if (!SUCCESS_PAYMENT_STATUS.equals(event.getPaymentStatus())) {
            publishInventoryFailure(event, "Payment status is not successful");
            markProcessed(event);
            return;
        }

        if (event.getOrderId().startsWith(INVENTORY_FAILURE_ORDER_PREFIX)) {
            publishInventoryFailure(event, "Inventory is not available");
            markProcessed(event);
            return;
        }

        log.info("Updating inventory for orderId={}", event.getOrderId());

        EventMetadata metadata = EventMetadata.from(event, "inventory-service");
        InventoryUpdatedEvent inventoryEvent = new InventoryUpdatedEvent(
                event.getOrderId(),
                UPDATED_STATUS,
                metadata.eventId(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.occurredAt(),
                metadata.producer(),
                metadata.schemaVersion()
        );

        inventoryProducer.send(inventoryEvent);
        markProcessed(event);
    }

    private void markProcessed(PaymentProcessedEvent event) {
        processedEventRepository.save(new ProcessedEvent(event.getEventId(), event.getOrderId()));
    }

    private void publishInventoryFailure(PaymentProcessedEvent event, String reason) {
        EventMetadata metadata = EventMetadata.from(event, "inventory-service");
        InventoryFailedEvent failedEvent = new InventoryFailedEvent(
                event.getOrderId(),
                FAILED_STATUS,
                reason,
                metadata.eventId(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.occurredAt(),
                metadata.producer(),
                metadata.schemaVersion()
        );

        inventoryProducer.sendFailure(failedEvent);
        log.warn("Inventory update failed for orderId={}: {}", event.getOrderId(), reason);
    }
}
