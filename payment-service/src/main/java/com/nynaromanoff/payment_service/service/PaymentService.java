
package com.nynaromanoff.payment_service.service;

import com.nynaromanoff.payment_service.dto.OrderCreatedEvent;
import com.nynaromanoff.payment_service.model.Payment;
import com.nynaromanoff.payment_service.model.PaymentStatus;
import com.nynaromanoff.payment_service.repository.PaymentRepository;
import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;
    private final StripeService stripeService;

    public PaymentService(PaymentRepository repository, StripeService stripeService) {
        this.repository = repository;
        this.stripeService = stripeService;
    }

    @Transactional
    public void processOrderCreatedEvent(OrderCreatedEvent event) throws Exception {

        if (repository.findByOrderId(event.orderId()).isPresent()) {
            log.info("Pagamento já existe para o pedido {}. Evento ignorado.", event.orderId());
            return;
        }

        if (event.totalValue() == null || event.totalValue().signum() <= 0) {
            throw new IllegalArgumentException("Valor inválido no evento do pedido " + event.orderId());
        }

        PaymentIntent intent = stripeService.createPaymentIntent(event.totalValue(), event.orderId());

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .value(event.totalValue())
                .status(PaymentStatus.PENDING)
                .paymentIntentId(intent.getId())
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(payment);
        log.info("Pagamento pendente criado. orderId={}, paymentId={}, intentId={}", event.orderId(), payment.getId(), intent.getId());
    }
}
