package com.apv.chronotrack.controller;

import com.apv.chronotrack.auth.AuthService;
import com.apv.chronotrack.models.Payment;
import com.apv.chronotrack.repository.PaymentRepository;
import com.apv.chronotrack.service.InvitationService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final InvitationService invitationService;
    private final AuthService authService;
    private final PaymentRepository paymentRepository;

    public WebhookController(InvitationService invitationService,
                                    AuthService authService,
                                   PaymentRepository paymentRepository) {
        this.invitationService = invitationService;
        this.authService = authService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) throws EventDataObjectDeserializationException {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Firma de webhook invalida: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.warn("Error deserializando webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error general");
        }

        log.info("Evento recibido de Stripe: {}", event.getType());

        StripeObject stripeObject = null;

        if (event.getDataObjectDeserializer().getObject().isPresent()) {
            stripeObject = event.getDataObjectDeserializer().getObject().get();
        } else {
            log.warn("Version de API diferente detectada. Intentando deserializacion forzada...");
            stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
        }

        if (stripeObject == null) {
            log.error("No se pudo leer el objeto del evento Stripe");
            return ResponseEntity.ok("Ignorado (Null)");
        }

        try {
            switch (event.getType()) {

                case "checkout.session.completed":
                    if (stripeObject instanceof com.stripe.model.checkout.Session) {
                        log.info("Procesando Checkout...");
                        handleCheckoutCompleted((com.stripe.model.checkout.Session) stripeObject);
                    }
                    break;

                case "invoice.payment_succeeded":
                    if (stripeObject instanceof Invoice) {
                        log.info("Procesando Factura...");
                        handleInvoicePayment((Invoice) stripeObject);
                    }
                    break;

                case "customer.subscription.updated":
                    if (stripeObject instanceof Subscription) {
                        handleSubscriptionUpdated((Subscription) stripeObject);
                    }
                    break;

                case "customer.subscription.deleted":
                    if (stripeObject instanceof Subscription) {
                        handleSubscriptionDeleted((Subscription) stripeObject);
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            log.error("Error procesando evento Stripe [{}]: {}", event.getType(), e.getMessage(), e);
        }

        return ResponseEntity.ok("Recibido");
    }

    private void handleCheckoutCompleted(com.stripe.model.checkout.Session session) {
        String clientReferenceId = session.getClientReferenceId();
        String customerEmail = session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null;
        String stripeCustomerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        log.info("Datos del checkout recibidos - Email: {}, Plan ID: {}, Customer ID: {}, Subscription ID: {}",
                customerEmail, clientReferenceId, stripeCustomerId, subscriptionId);

        if (customerEmail == null || clientReferenceId == null) {
            log.error("Email o Plan ID son nulos. No se puede activar la cuenta. Revisa que el frontend envie 'priceId' en 'clientReferenceId'");
            return;
        }

        try {
            log.info("Iniciando activacion de servicio para: {}", customerEmail);
            invitationService.createAndSendInvitation(customerEmail, clientReferenceId, stripeCustomerId, subscriptionId);
            log.info("Servicio activado e invitacion enviada correctamente para: {}", customerEmail);
        } catch (Exception e) {
            log.error("Error en InvitationService para {}: {}", customerEmail, e.getMessage(), e);
        }
    }

    private void handleSubscriptionUpdated(Subscription subscription) {
        String stripeCustomerId = subscription.getCustomer();
        String status = subscription.getStatus();

        String newPriceId = null;
        if (!subscription.getItems().getData().isEmpty()) {
            newPriceId = subscription.getItems().getData().get(0).getPrice().getId();
        }

        authService.updateSubscriptionStatus(stripeCustomerId, status, newPriceId);
        log.info("Suscripcion actualizada: {} -> {}", stripeCustomerId, status);
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        String stripeCustomerId = subscription.getCustomer();
        authService.updateSubscriptionStatus(stripeCustomerId, "canceled", null);
        log.info("Suscripcion cancelada: {}", stripeCustomerId);
    }

    private void handleInvoicePayment(Invoice invoice) {
        Payment payment = new Payment();
        payment.setStripeSessionId(invoice.getId());
        payment.setAmount(invoice.getAmountPaid() / 100.0);
        payment.setCurrency(invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase() : "USD");
        payment.setCustomerEmail(invoice.getCustomerEmail());

        if (invoice.getAmountPaid() == 0) {
            payment.setStatus("trial_started");
            log.info("Trial iniciado para: {}", invoice.getCustomerEmail());
        } else {
            payment.setStatus("succeeded");
            log.info("Pago registrado de: ${}", payment.getAmount());
        }

        paymentRepository.save(payment);
    }
}
