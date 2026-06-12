package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.events.InventoryFailedEvent;
import com.example.events.InventoryUpdatedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.inventoryservice.kafka.InventoryProducer;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryProducer inventoryProducer;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void updateInventoryPublishesInventoryUpdatedEvent() {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                "order-1",
                "SUCCESS",
                "customer@example.com",
                "payment-processed-event-1",
                "correlation-1",
                "order-created-event-1",
                "2026-06-12T10:00:00Z",
                "payment-service",
                "1"
        );
        when(processedEventRepository.existsById(event.getOrderId())).thenReturn(false);

        inventoryService.updateInventory(event);

        ArgumentCaptor<InventoryUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(InventoryUpdatedEvent.class);
        ArgumentCaptor<ProcessedEvent> processedEventCaptor =
                ArgumentCaptor.forClass(ProcessedEvent.class);

        verify(inventoryProducer).send(eventCaptor.capture());
        verify(processedEventRepository).save(processedEventCaptor.capture());

        InventoryUpdatedEvent inventoryEvent = eventCaptor.getValue();
        assertThat(inventoryEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(inventoryEvent.getStatus()).isEqualTo("UPDATED");
        assertThat(processedEventCaptor.getValue().getOrderId()).isEqualTo(event.getOrderId());
    }

    @Test
    void updateInventoryPublishesFailureEventWhenInventoryIsUnavailable() {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                "fail-inventory-order-1",
                "SUCCESS",
                "customer@example.com",
                "payment-processed-event-1",
                "correlation-1",
                "order-created-event-1",
                "2026-06-12T10:00:00Z",
                "payment-service",
                "1"
        );
        when(processedEventRepository.existsById(event.getOrderId())).thenReturn(false);

        inventoryService.updateInventory(event);

        ArgumentCaptor<InventoryFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(InventoryFailedEvent.class);

        verify(inventoryProducer).sendFailure(eventCaptor.capture());
        verify(inventoryProducer, never()).send(org.mockito.ArgumentMatchers.any());

        InventoryFailedEvent failedEvent = eventCaptor.getValue();
        assertThat(failedEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(failedEvent.getStatus()).isEqualTo("FAILED");
        assertThat(failedEvent.getReason()).isEqualTo("Inventory is not available");
    }

    @Test
    void updateInventorySkipsAlreadyProcessedEvent() {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                "order-1",
                "SUCCESS",
                "customer@example.com",
                "payment-processed-event-1",
                "correlation-1",
                "order-created-event-1",
                "2026-06-12T10:00:00Z",
                "payment-service",
                "1"
        );
        when(processedEventRepository.existsById(event.getOrderId())).thenReturn(true);

        inventoryService.updateInventory(event);

        verify(inventoryProducer, never()).send(org.mockito.ArgumentMatchers.any());
        verify(inventoryProducer, never()).sendFailure(org.mockito.ArgumentMatchers.any());
        verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
