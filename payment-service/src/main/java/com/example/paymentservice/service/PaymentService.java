package com.example.paymentservice.service;

import com.example.events.InventoryFailedEvent;
import com.example.events.OrderCreatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.events.PaymentRefundedEvent;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.entity.PaymentTransaction;
import com.example.paymentservice.entity.ProcessedEvent;
import com.example.paymentservice.event.EventMetadata;
import com.example.paymentservice.metrics.PaymentMetrics;
import com.example.paymentservice.repository.PaymentTransactionRepository;
import com.example.paymentservice.repository.ProcessedEventRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private static final int PAYMENT_FAILURE_THRESHOLD = 500;
  private static final String SUCCESS_STATUS = "SUCCESS";
  private static final String FAILED_STATUS = "FAILED";
  private static final String REFUNDED_STATUS = "REFUNDED";

  private final PaymentOutboxService paymentOutboxService;
  private final PaymentMetrics paymentMetrics;
  private final ProcessedEventRepository processedEventRepository;
  private final PaymentTransactionRepository paymentTransactionRepository;

  @Transactional
  public void processPayment(OrderCreatedEvent event) {
    PaymentTransaction transaction = reservePayment(event);
    if (transaction == null) {
      paymentMetrics.incrementDuplicatePaymentSkipped();
      log.warn("Already processed orderId={}", event.getOrderId());
      return;
    }

    BigDecimal amount = new BigDecimal(event.getAmount());

    if (amount.intValue() > PAYMENT_FAILURE_THRESHOLD) {
      String reason = "Payment amount exceeds threshold";
      EventMetadata metadata = EventMetadata.from(event, "payment-service");
      PaymentFailedEvent failedEvent =
          new PaymentFailedEvent(
              event.getOrderId(),
              FAILED_STATUS,
              reason,
              event.getCustomerEmail(),
              metadata.eventId(),
              metadata.correlationId(),
              metadata.causationId(),
              metadata.occurredAt(),
              metadata.producer(),
              metadata.schemaVersion());

      transaction.markFailed(reason);
      paymentTransactionRepository.save(transaction);
      paymentOutboxService.enqueuePaymentFailed(event.getOrderId(), failedEvent);
      processedEventRepository.save(new ProcessedEvent(event.getEventId(), event.getOrderId()));
      paymentMetrics.incrementPaymentFailed();
      log.warn("Payment failed for orderId={}", event.getOrderId());
      return;
    }

    log.info("Processing payment for orderId={}", event.getOrderId());

    EventMetadata metadata = EventMetadata.from(event, "payment-service");
    PaymentProcessedEvent paymentEvent =
        new PaymentProcessedEvent(
            event.getOrderId(),
            SUCCESS_STATUS,
            event.getCustomerEmail(),
            metadata.eventId(),
            metadata.correlationId(),
            metadata.causationId(),
            metadata.occurredAt(),
            metadata.producer(),
            metadata.schemaVersion());

    transaction.markSuccess();
    paymentTransactionRepository.save(transaction);
    paymentOutboxService.enqueuePaymentProcessed(event.getOrderId(), paymentEvent);
    processedEventRepository.save(new ProcessedEvent(event.getEventId(), event.getOrderId()));
    paymentMetrics.incrementPaymentSucceeded();

    log.info("Payment processed successfully for orderId={}", event.getOrderId());
  }

  @Transactional
  public void compensatePayment(InventoryFailedEvent event) {
    if (processedEventRepository.existsById(event.getEventId())) {
      log.warn(
          "Already processed compensation event eventId={} orderId={}",
          event.getEventId(),
          event.getOrderId());
      return;
    }

    PaymentTransaction transaction =
        paymentTransactionRepository.findById(event.getOrderId()).orElse(null);

    if (transaction == null) {
      log.warn("Skipping compensation for unknown orderId={}", event.getOrderId());
      processedEventRepository.save(new ProcessedEvent(event.getEventId(), event.getOrderId()));
      return;
    }

    if (PaymentStatus.REFUNDED.equals(transaction.getStatus())) {
      log.warn("Payment already refunded for orderId={}", event.getOrderId());
      processedEventRepository.save(new ProcessedEvent(event.getEventId(), event.getOrderId()));
      return;
    }

    log.warn(
        "Compensating payment for orderId={} because inventory failed: {}",
        event.getOrderId(),
        event.getReason());

    EventMetadata metadata = EventMetadata.from(event, "payment-service");
    PaymentRefundedEvent refundedEvent =
        new PaymentRefundedEvent(
            event.getOrderId(),
            REFUNDED_STATUS,
            "Inventory reservation failed: " + event.getReason(),
            metadata.eventId(),
            metadata.correlationId(),
            metadata.causationId(),
            metadata.occurredAt(),
            metadata.producer(),
            metadata.schemaVersion());

    transaction.markRefunded(refundedEvent.getReason());
    paymentTransactionRepository.save(transaction);
    paymentOutboxService.enqueuePaymentRefunded(event.getOrderId(), refundedEvent);
    processedEventRepository.save(new ProcessedEvent(event.getEventId(), event.getOrderId()));
    paymentMetrics.incrementPaymentRefunded();
  }

  private PaymentTransaction reservePayment(OrderCreatedEvent event) {
    if (processedEventRepository.existsById(event.getEventId())) {
      return null;
    }

    try {
      return paymentTransactionRepository.saveAndFlush(
          new PaymentTransaction(
              event.getOrderId(), new BigDecimal(event.getAmount()), event.getCustomerEmail()));
    } catch (DataIntegrityViolationException ex) {
      log.warn(
          "Duplicate payment request rejected by unique orderId guard: {}", event.getOrderId());
      return null;
    }
  }
}
