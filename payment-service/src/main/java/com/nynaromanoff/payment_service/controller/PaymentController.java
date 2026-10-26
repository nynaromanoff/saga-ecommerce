
package com.nynaromanoff.payment_service.controller;

import com.nynaromanoff.payment_service.dto.PaymentIntentResponse;
import com.nynaromanoff.payment_service.repository.PaymentRepository;
import com.nynaromanoff.payment_service.service.StripeService;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentRepository repository;
    private final StripeService stripeService;

    public PaymentController(PaymentRepository repository, StripeService stripeService) {
        this.repository = repository;
        this.stripeService = stripeService;
    }

    @GetMapping("/orders/{orderId}/intent")
    public ResponseEntity<PaymentIntentResponse> getPaymentIntent(@PathVariable UUID orderId) throws Exception {

        var payment = repository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pagamento ainda não foi inicializado para este pedido."));

        if (payment.getPaymentIntentId() == null) {
            throw new
                    ResponseStatusException(
                            HttpStatus.CONFLICT,
                    "PaymentIntent ainda não está disponível.");
        }

        PaymentIntent intent = stripeService.retrievePaymentIntent(payment.getPaymentIntentId());

        return ResponseEntity.ok(new PaymentIntentResponse(intent.getClientSecret()));
    }
}
