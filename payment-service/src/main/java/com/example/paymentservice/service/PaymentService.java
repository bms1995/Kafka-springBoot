package com.example.paymentservice.service;

import com.example.paymentservice.entity.ProcessedEvent;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.entity.PaymentTransaction;
import com.example.events.InventoryFailedEvent;
import com.example.events.OrderCreatedEvent;
import com.example.events.PaymentFailedEvent;
import com.example.events.PaymentProcessedEvent;
import com.example.events.PaymentRefundedEvent;
import com.example.paymentservice.repository.PaymentTransactionRepository;
import com.example.paymentservice.repository.ProcessedEventRepository;
import com.example.paymentservice.metrics.PaymentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    event.getOrderId(),
                    FAILED_STATUS,
                    reason,
                    event.getCustomerEmail()
            );

            transaction.markFailed(reason);
            paymentTransactionRepository.save(transaction);
            paymentOutboxService.enqueue(PaymentOutboxService.PAYMENT_FAILED_TOPIC, event.getOrderId(), failedEvent);
            processedEventRepository.save(new ProcessedEvent(event.getOrderId()));
            paymentMetrics.incrementPaymentFailed();
            log.warn("Payment failed for orderId={}", event.getOrderId());
            return;
        }

        log.info("Processing payment for orderId={}", event.getOrderId());

        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
                event.getOrderId(),
                SUCCESS_STATUS,
                event.getCustomerEmail()
        );

        transaction.markSuccess();
        paymentTransactionRepository.save(transaction);
        paymentOutboxService.enqueue(PaymentOutboxService.PAYMENT_PROCESSED_TOPIC, event.getOrderId(), paymentEvent);
        processedEventRepository.save(new ProcessedEvent(event.getOrderId()));
        paymentMetrics.incrementPaymentSucceeded();

        log.info("Payment processed successfully for orderId={}", event.getOrderId());
    }

    @Transactional
    public void compensatePayment(InventoryFailedEvent event) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(event.getOrderId())
                .orElse(null);

        if (transaction == null) {
            log.warn("Skipping compensation for unknown orderId={}", event.getOrderId());
            return;
        }

        if (PaymentStatus.REFUNDED.equals(transaction.getStatus())) {
            log.warn("Payment already refunded for orderId={}", event.getOrderId());
            return;
        }

        log.warn("Compensating payment for orderId={} because inventory failed: {}",
                event.getOrderId(),
                event.getReason());

        PaymentRefundedEvent refundedEvent = new PaymentRefundedEvent(
                event.getOrderId(),
                REFUNDED_STATUS,
                "Inventory reservation failed: " + event.getReason()
        );

        transaction.markRefunded(refundedEvent.getReason());
        paymentTransactionRepository.save(transaction);
        paymentOutboxService.enqueue(PaymentOutboxService.PAYMENT_REFUNDED_TOPIC, event.getOrderId(), refundedEvent);
        paymentMetrics.incrementPaymentRefunded();
    }

    private PaymentTransaction reservePayment(OrderCreatedEvent event) {
        if (processedEventRepository.existsById(event.getOrderId())) {
            return null;
        }

        try {
            return paymentTransactionRepository.saveAndFlush(
                    new PaymentTransaction(event.getOrderId(), new BigDecimal(event.getAmount()), event.getCustomerEmail())
            );
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate payment request rejected by unique orderId guard: {}", event.getOrderId());
            return null;
        }
    }
}
