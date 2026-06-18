package com.example.paymentservice.service;

import com.example.paymentservice.entity.ProcessedEvent;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.entity.PaymentTransaction;
import com.example.events.InventoryFailedEvent;
import com.example.events.OrderCreatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.events.PaymentRefundedEvent;
import com.example.paymentservice.metrics.PaymentMetrics;
import com.example.paymentservice.repository.PaymentTransactionRepository;
import com.example.paymentservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentOutboxService paymentOutboxService;

    @Mock
    private PaymentMetrics paymentMetrics;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPaymentPublishesSuccessEventAndMarksOrderAsProcessed() {
        OrderCreatedEvent event = orderCreatedEvent("order-1", BigDecimal.valueOf(250));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(paymentTransactionRepository.saveAndFlush(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPayment(event);

        ArgumentCaptor<PaymentProcessedEvent> paymentEventCaptor =
                ArgumentCaptor.forClass(PaymentProcessedEvent.class);
        ArgumentCaptor<ProcessedEvent> processedEventCaptor =
                ArgumentCaptor.forClass(ProcessedEvent.class);
        ArgumentCaptor<PaymentTransaction> transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);

        verify(paymentOutboxService).enqueuePaymentProcessed(
                org.mockito.ArgumentMatchers.eq(event.getOrderId()),
                paymentEventCaptor.capture()
        );
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        verify(processedEventRepository).save(processedEventCaptor.capture());

        PaymentProcessedEvent paymentEvent = paymentEventCaptor.getValue();
        PaymentTransaction transaction = transactionCaptor.getValue();
        assertThat(paymentEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(paymentEvent.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(paymentEvent.getCustomerEmail()).isEqualTo(event.getCustomerEmail());
        assertThat(transaction.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(event.getEventId());
        assertThat(processedEventCaptor.getValue().getOrderId()).isEqualTo(event.getOrderId());
    }

    @Test
    void processPaymentSkipsAlreadyProcessedOrder() {
        OrderCreatedEvent event = orderCreatedEvent("order-1", BigDecimal.valueOf(250));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(true);

        paymentService.processPayment(event);

        verify(paymentOutboxService, never()).enqueuePaymentProcessed(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(paymentTransactionRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void processPaymentPublishesFailureEventWhenAmountExceedsThreshold() {
        OrderCreatedEvent event = orderCreatedEvent("order-1", BigDecimal.valueOf(501));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(paymentTransactionRepository.saveAndFlush(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPayment(event);

        ArgumentCaptor<PaymentFailedEvent> failedEventCaptor =
                ArgumentCaptor.forClass(PaymentFailedEvent.class);
        ArgumentCaptor<ProcessedEvent> processedEventCaptor =
                ArgumentCaptor.forClass(ProcessedEvent.class);
        ArgumentCaptor<PaymentTransaction> transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);

        verify(paymentOutboxService).enqueuePaymentFailed(
                org.mockito.ArgumentMatchers.eq(event.getOrderId()),
                failedEventCaptor.capture()
        );
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        verify(processedEventRepository).save(processedEventCaptor.capture());

        PaymentFailedEvent failedEvent = failedEventCaptor.getValue();
        PaymentTransaction transaction = transactionCaptor.getValue();
        assertThat(failedEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(failedEvent.getStatus()).isEqualTo("FAILED");
        assertThat(failedEvent.getCustomerEmail()).isEqualTo(event.getCustomerEmail());
        assertThat(transaction.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(event.getEventId());
        assertThat(processedEventCaptor.getValue().getOrderId()).isEqualTo(event.getOrderId());
    }

    @Test
    void compensatePaymentPublishesRefundEvent() {
        InventoryFailedEvent event = new InventoryFailedEvent(
                "order-1",
                "FAILED",
                "Inventory is not available",
                "inventory-failed-event-1",
                "correlation-1",
                "payment-processed-event-1",
                "2026-06-12T10:00:00Z",
                "inventory-service",
                "1"
        );
        PaymentTransaction transaction = new PaymentTransaction(
                event.getOrderId(),
                BigDecimal.valueOf(250),
                "customer@example.com"
        );
        transaction.markSuccess();
        when(paymentTransactionRepository.findById(event.getOrderId()))
                .thenReturn(Optional.of(transaction));

        paymentService.compensatePayment(event);

        ArgumentCaptor<PaymentRefundedEvent> refundedEventCaptor =
                ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        ArgumentCaptor<PaymentTransaction> transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);
        ArgumentCaptor<ProcessedEvent> processedEventCaptor =
                ArgumentCaptor.forClass(ProcessedEvent.class);

        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        verify(processedEventRepository).save(processedEventCaptor.capture());
        verify(paymentOutboxService).enqueuePaymentRefunded(
                org.mockito.ArgumentMatchers.eq(event.getOrderId()),
                refundedEventCaptor.capture()
        );

        PaymentRefundedEvent refundedEvent = refundedEventCaptor.getValue();
        assertThat(refundedEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(refundedEvent.getStatus()).isEqualTo("REFUNDED");
        assertThat(refundedEvent.getReason()).contains(event.getReason());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(event.getEventId());
        assertThat(processedEventCaptor.getValue().getOrderId()).isEqualTo(event.getOrderId());
    }

    @Test
    void compensatePaymentSkipsAlreadyRefundedPayment() {
        InventoryFailedEvent event = new InventoryFailedEvent(
                "order-1",
                "FAILED",
                "Inventory is not available",
                "inventory-failed-event-1",
                "correlation-1",
                "payment-processed-event-1",
                "2026-06-12T10:00:00Z",
                "inventory-service",
                "1"
        );
        PaymentTransaction transaction = new PaymentTransaction(
                event.getOrderId(),
                BigDecimal.valueOf(250),
                "customer@example.com"
        );
        transaction.markRefunded("already refunded");
        when(paymentTransactionRepository.findById(event.getOrderId()))
                .thenReturn(Optional.of(transaction));

        paymentService.compensatePayment(event);

        verify(paymentOutboxService, never()).enqueuePaymentRefunded(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(paymentTransactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
        ArgumentCaptor<ProcessedEvent> processedEventCaptor =
                ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(event.getEventId());
        assertThat(processedEventCaptor.getValue().getOrderId()).isEqualTo(event.getOrderId());
    }

    @Test
    void compensatePaymentSkipsAlreadyProcessedCompensationEvent() {
        InventoryFailedEvent event = new InventoryFailedEvent(
                "order-1",
                "FAILED",
                "Inventory is not available",
                "inventory-failed-event-1",
                "correlation-1",
                "payment-processed-event-1",
                "2026-06-12T10:00:00Z",
                "inventory-service",
                "1"
        );
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(true);

        paymentService.compensatePayment(event);

        verify(paymentTransactionRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(paymentOutboxService, never()).enqueuePaymentRefunded(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private OrderCreatedEvent orderCreatedEvent(String orderId, BigDecimal amount) {
        return new OrderCreatedEvent(
                orderId,
                "Laptop",
                1,
                amount.toPlainString(),
                "customer@example.com",
                "order-created-event-1",
                "correlation-1",
                "causation-1",
                "2026-06-12T10:00:00Z",
                "order-service",
                "1"
        );
    }
}
