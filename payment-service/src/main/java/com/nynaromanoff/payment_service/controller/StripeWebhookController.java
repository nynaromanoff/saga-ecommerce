
package com.nynaromanoff.payment_service.controller;

import com.nynaromanoff.payment_service.service.StripeWebhookService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class StripeWebhookController {

    private final StripeWebhookService webhookService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(StripeWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature",
                    required = false) String signature
    ) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        final Event event;

        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException exception) {
            log.warn("Assinatura inválida recebida no webhook Stripe.");
            return ResponseEntity.badRequest().build();
        } catch (Exception exception) {
            log.warn("Não foi possível interpretar o webhook Stripe.");
            return ResponseEntity.badRequest().build();
        }

        String eventType = event.getType();

        if (!"payment_intent.succeeded".equals(eventType)
                && !"payment_intent.payment_failed".equals(eventType)) {
            log.info("Evento Stripe não utilizado: {}", eventType);
            return ResponseEntity.ok().build();
        }

        try {

            var deserializer = event.getDataObjectDeserializer();

            StripeObject object;

            if (deserializer.getObject().isPresent()) {
                object = deserializer.getObject().get();
            } else {
                log.warn(
                        "Versão da API Stripe incompatível com a desserialização segura. " +
                                "eventId={}, eventApiVersion={}, libraryApiVersion={}",
                        event.getId(),
                        event.getApiVersion(),
                        com.stripe.Stripe.API_VERSION
                );

                try {
                    object = deserializer.deserializeUnsafe();
                } catch (EventDataObjectDeserializationException exception) {
                    log.error("Falha na desserialização do evento Stripe {}", event.getId(), exception);

                    return ResponseEntity.internalServerError().build();
                }
            }

            if (!(object instanceof PaymentIntent intent)) {
                log.warn("Objeto inesperado no evento Stripe {}: {}", event.getType(), object.getClass().getName());
                return ResponseEntity.badRequest().build();
            }

            webhookService.process(event, intent);
            return ResponseEntity.ok().build();

        } catch (Exception exception) {
            log.error("Falha ao processar evento Stripe {}. Será solicitado reenvio.", event.getId(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
