package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.events.InventoryFailedEvent;
import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.inventoryservice.kafka.InventoryProducer;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public void updateInventory(PaymentProcessedEvent event) {
        if (processedEventRepository.existsById(event.getOrderId())) {
            log.warn("Already updated inventory for orderId={}", event.getOrderId());
            return;
        }

        if (!SUCCESS_PAYMENT_STATUS.equals(event.getPaymentStatus())) {
            publishInventoryFailure(event.getOrderId(), "Payment status is not successful");
            processedEventRepository.save(new ProcessedEvent(event.getOrderId()));
            return;
        }

        if (event.getOrderId().startsWith(INVENTORY_FAILURE_ORDER_PREFIX)) {
            publishInventoryFailure(event.getOrderId(), "Inventory is not available");
            processedEventRepository.save(new ProcessedEvent(event.getOrderId()));
            return;
        }

        log.info("Updating inventory for orderId={}", event.getOrderId());

        InventoryUpdatedEvent inventoryEvent = new InventoryUpdatedEvent(
                event.getOrderId(),
                UPDATED_STATUS
        );

        inventoryProducer.send(inventoryEvent);
        processedEventRepository.save(new ProcessedEvent(event.getOrderId()));
    }

    private void publishInventoryFailure(String orderId, String reason) {
        InventoryFailedEvent failedEvent = new InventoryFailedEvent(
                orderId,
                FAILED_STATUS,
                reason
        );

        inventoryProducer.sendFailure(failedEvent);
        log.warn("Inventory update failed for orderId={}: {}", orderId, reason);
    }
}
