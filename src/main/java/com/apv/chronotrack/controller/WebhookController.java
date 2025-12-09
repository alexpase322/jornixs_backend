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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

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
            // A. Verificación de Seguridad (CRÍTICO)
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("⚠️ FIRMA INVÁLIDA: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");
        } catch (Exception e) {
            System.err.println("⚠️ Error deserializando webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error general");
        }

        // B. Log de diagnóstico (Para ver si entra)
        System.out.println("⚡ Evento recibido de Stripe: " + event.getType());

        StripeObject stripeObject = null;

        if (event.getDataObjectDeserializer().getObject().isPresent()) {
            // Caso ideal: Las versiones coinciden
            stripeObject = event.getDataObjectDeserializer().getObject().get();
        } else {
            // Caso común: Conflicto de versiones. Forzamos la lectura.
            System.out.println("⚠️ Versión de API diferente detectada. Intentando deserialización forzada...");
            stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
        }

        // Si después de forzarlo sigue siendo null, entonces sí lo ignoramos
        if (stripeObject == null) {
            System.out.println("❌ Error: No se pudo leer el objeto del evento ni siquiera forzándolo.");
            return ResponseEntity.ok("Ignorado (Null)");
        }

        try {
            switch (event.getType()) {

                // --- CASO 1: ACTIVACIÓN DE CUENTA (Checkout completado) ---
                case "checkout.session.completed":
                    if (stripeObject instanceof com.stripe.model.checkout.Session) {
                        System.out.println("🕵️‍♂️ Procesando Checkout...");
                        handleCheckoutCompleted((com.stripe.model.checkout.Session) stripeObject);
                    }
                    break;

                // --- CASO 2: DINERO ENTRANDO (Pago mensual o Trial iniciado) ---
                case "invoice.payment_succeeded":
                    if (stripeObject instanceof Invoice) {
                        System.out.println("🕵️‍♂️ Procesando Factura...");
                        handleInvoicePayment((Invoice) stripeObject);
                    }
                    break;

                // --- CASO 3: CAMBIO DE PLAN EN EL PORTAL ---
                case "customer.subscription.updated":
                    if (stripeObject instanceof Subscription) {
                        handleSubscriptionUpdated((Subscription) stripeObject);
                    }
                    break;

                // --- CASO 4: CANCELACIÓN ---
                case "customer.subscription.deleted":
                    if (stripeObject instanceof Subscription) {
                        handleSubscriptionDeleted((Subscription) stripeObject);
                    }
                    break;

                default:
                    // Ignoramos eventos que no configuramos
                    break;
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR FATAL DENTRO DEL SWITCH: " + e.getMessage());
            e.printStackTrace();
            // Devolvemos OK para no bloquear la cola de Stripe con reintentos infinitos si es error de lógica
        }

        return ResponseEntity.ok("Recibido");
    }

    // ================= MÉTODOS PRIVADOS DE LÓGICA =================

    private void handleCheckoutCompleted(com.stripe.model.checkout.Session session) {
        // Extraemos los datos con cuidado
        String clientReferenceId = session.getClientReferenceId(); // Aquí viene el ID del plan (price_...)
        String customerEmail = session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null;
        String stripeCustomerId = session.getCustomer(); // cus_xxxx
        String subscriptionId = session.getSubscription(); // sub_xxxx

        // --- LOGS DETALLADOS PARA DEPURAR (Míralos en Render) ---
        System.out.println("📝 DATOS DEL CHECKOUT RECIBIDOS:");
        System.out.println("   > Email: " + customerEmail);
        System.out.println("   > Plan ID (Reference): " + clientReferenceId);
        System.out.println("   > Customer ID: " + stripeCustomerId);
        System.out.println("   > Subscription ID: " + subscriptionId);

        // Validación crítica
        if (customerEmail == null || clientReferenceId == null) {
            System.err.println("⚠️ ALERTA: Email o Plan ID son NULOS. No se puede activar la cuenta.");
            System.err.println("   (Consejo: Revisa que el frontend esté enviando 'priceId' en 'clientReferenceId')");
            return;
        }

        try {
            System.out.println("🚀 Iniciando activación de servicio para: " + customerEmail);

            // LLAMADA AL SERVICIO
            invitationService.createAndSendInvitation(customerEmail, clientReferenceId, stripeCustomerId, subscriptionId);

            System.out.println("✅ Servicio activado e invitación enviada correctamente.");

        } catch (Exception e) {
            System.err.println("❌ ERROR EN INVITATION SERVICE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSubscriptionUpdated(Subscription subscription) {
        String stripeCustomerId = subscription.getCustomer();
        String status = subscription.getStatus();

        // Recuperar el plan actual (útil si cambió en el portal)
        String newPriceId = null;
        if (!subscription.getItems().getData().isEmpty()) {
            newPriceId = subscription.getItems().getData().get(0).getPrice().getId();
        }

        authService.updateSubscriptionStatus(stripeCustomerId, status, newPriceId);
        System.out.println("🔄 Suscripción actualizada: " + stripeCustomerId + " -> " + status);
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        String stripeCustomerId = subscription.getCustomer();
        authService.updateSubscriptionStatus(stripeCustomerId, "canceled", null);
        System.out.println("❌ Suscripción cancelada: " + stripeCustomerId);
    }


    private void handleInvoicePayment(Invoice invoice) {
        Payment payment = new Payment();
        payment.setStripeSessionId(invoice.getId());
        payment.setAmount(invoice.getAmountPaid() / 100.0);
        payment.setCurrency(invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase() : "USD");
        payment.setCustomerEmail(invoice.getCustomerEmail());

        // Manejo de Trial (Monto 0)
        if (invoice.getAmountPaid() == 0) {
            payment.setStatus("trial_started");
            System.out.println("🎁 Trial iniciado para: " + invoice.getCustomerEmail());
        } else {
            payment.setStatus("succeeded");
            System.out.println("💰 Pago registrado de: " + payment.getAmount());
        }

        paymentRepository.save(payment);
    }
}
