package com.nynaromanoff.payment_service.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = this.secretKey;
    }

    public boolean processCardPayment(BigDecimal value, String orderId) {
        try {
            long amountInCents = value.multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("brl") // Define a moeda como Real Brasileiro
                    // Em produção, você capturaria o token do cartão vindo do front-end React
                    .setPaymentMethod("pm_card_chargeDeclined") // Token oficial de teste da Stripe para cartão Visa válido
                    .setConfirm(true) // Confirma o pagamento imediatamente
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .putMetadata("order_id", orderId) // Vincula o ID do seu pedido nas propriedades da Stripe
                    .build();

            // Dispara a requisição HTTP real para os servidores da Stripe!
            PaymentIntent intent = PaymentIntent.create(params);

            log.info("💰 [Stripe] Resposta recebida da API externa. ID da Transação: {}. Status: {}",
                    intent.getId(), intent.getStatus());

            // Retorna verdadeiro se o status for "succeeded" (pago com sucesso)
            return "succeeded".equalsIgnoreCase(intent.getStatus());

        } catch (Exception e) {
            log.error("❌ [Stripe] Falha crítica ao processar cobrança na API externa para o pedido {}", orderId, e);
            return false; // Retorna falso se o cartão for recusado ou a API falhar
        }
    }
}
