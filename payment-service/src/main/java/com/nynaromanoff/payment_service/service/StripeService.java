
package com.nynaromanoff.payment_service.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public PaymentIntent createPaymentIntent(BigDecimal value, UUID orderId) throws Exception {
        if (value == null || value.signum() <= 0) {throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero.");}

        long amountInCents = value.movePointRight(2).longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("brl")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams
                                .AutomaticPaymentMethods
                                .builder()
                                .setEnabled(true)
                                .build()
                        )
                        .putMetadata("order_id", orderId.toString())
                        .build();

        PaymentIntent intent = PaymentIntent.create(params);

        log.info("[Stripe] PaymentIntent criado: orderId={}, intentId={}, status={}", orderId, intent.getId(), intent.getStatus());
        return intent;
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws Exception {
        return PaymentIntent.retrieve(paymentIntentId);
    }
}
