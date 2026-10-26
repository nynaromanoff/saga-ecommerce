
package com.nynaromanoff.payment_service.service;

import com.nynaromanoff.payment_service.dto.PaymentProcessedEvent;
import com.nynaromanoff.payment_service.model.Payment;
import com.nynaromanoff.payment_service.model.PaymentStatus;
import com.nynaromanoff.payment_service.producer.PaymentProducer;
import com.nynaromanoff.payment_service.repository.PaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class StripeWebhookService {

    private final PaymentRepository repository;
    private final PaymentProducer paymentProducer;

    public StripeWebhookService(PaymentRepository repository, PaymentProducer paymentProducer) {
        this.repository = repository;
        this.paymentProducer = paymentProducer;
    }

    @Transactional
    public void process(Event event, PaymentIntent intent) {
        Payment payment = repository.findByPaymentIntentId(intent.getId()).orElseThrow(
                () -> new IllegalArgumentException("Pagamento não encontrado para PaymentIntent: " + intent.getId()));

        PaymentStatus newStatus;

        switch (event.getType()) {
            case "payment_intent.succeeded" -> newStatus = PaymentStatus.APPROVED;
            case "payment_intent.payment_failed" -> newStatus = PaymentStatus.REJECTED;
            default -> {log.info("Evento Stripe ignorado: {}", event.getType());
                return;
            }
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Evento duplicado ou pagamento já finalizado. orderId={}, status={}", payment.getOrderId(), payment.getStatus());
            return;
        }

        payment.setStatus(newStatus);
        repository.save(payment);

        PaymentProcessedEvent result = new PaymentProcessedEvent(payment.getOrderId(), newStatus.name());
        paymentProducer.sendPaymentProcessedMessage(result);

        log.info("Pagamento atualizado: orderId={}, status={}, stripeEvent={}", payment.getOrderId(), newStatus, event.getId());
    }
}
