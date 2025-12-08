package com.apv.chronotrack.controller;

import com.apv.chronotrack.auth.AuthService;
import com.apv.chronotrack.models.Payment;
import com.apv.chronotrack.repository.PaymentRepository;
import com.apv.chronotrack.service.InvitationService;
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
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            // 1. VERIFICAR FIRMA DE SEGURIDAD (Obligatorio)
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            System.err.println("⚠️ Firma inválida en Webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error de firma");
        }

        // 2. OBTENER EL OBJETO DESERIALIZADO
        Optional<StripeObject> objectOptional = event.getDataObjectDeserializer().getObject();

        if (objectOptional.isEmpty()) {
            // A veces Stripe envía eventos antiguos que la librería no puede deserializar
            return ResponseEntity.ok("Evento ignorado (sin objeto)");
        }

        StripeObject stripeObject = objectOptional.get();

        // 3. SWITCH PARA MANEJAR CADA TIPO DE EVENTO
        try {
            switch (event.getType()) {

                // --- A: COMPRA INICIAL ---
                case "checkout.session.completed":
                    if (stripeObject instanceof com.stripe.model.checkout.Session) {
                        handleCheckoutCompleted((com.stripe.model.checkout.Session) stripeObject);
                    }
                    break;

                // --- B: CAMBIO DE PLAN O ESTADO (Desde el Portal) ---
                case "customer.subscription.updated":
                    if (stripeObject instanceof Subscription) {
                        handleSubscriptionUpdated((Subscription) stripeObject);
                    }
                    break;

                // --- C: CANCELACIÓN DEFINITIVA ---
                case "customer.subscription.deleted":
                    if (stripeObject instanceof Subscription) {
                        handleSubscriptionDeleted((Subscription) stripeObject);
                    }
                    break;

                // --- D: REGISTRO DE PAGO MENSUAL (Factura pagada) ---
                case "invoice.payment_succeeded":
                    if (stripeObject instanceof Invoice) {
                        handleInvoicePaymentSucceeded((Invoice) stripeObject);
                    }
                    break;

                default:
                    // Ignoramos eventos que no nos interesan
                    break;
            }
        } catch (Exception e) {
            System.err.println("❌ Error procesando evento " + event.getType() + ": " + e.getMessage());
            e.printStackTrace();
            // Retornamos 200 OK para que Stripe no siga reintentando si es un error de lógica nuestra
        }

        return ResponseEntity.ok("Recibido");
    }

    // ================= MÉTODOS PRIVADOS DE LÓGICA =================

    private void handleCheckoutCompleted(com.stripe.model.checkout.Session session) {
        String clientReferenceId = session.getClientReferenceId(); // ID del Plan
        String customerEmail = session.getCustomerDetails().getEmail();
        String stripeCustomerId = session.getCustomer(); // cus_...
        String subscriptionId = session.getSubscription(); // sub_...

        System.out.println("✅ Nueva Suscripción: " + customerEmail + " | Plan: " + clientReferenceId);

        // Llama a tu servicio existente para invitar/activar
        invitationService.createAndSendInvitation(customerEmail, clientReferenceId, stripeCustomerId, subscriptionId);
    }

    private void handleSubscriptionUpdated(Subscription subscription) {
        String stripeCustomerId = subscription.getCustomer();
        String status = subscription.getStatus(); // active, past_due, canceled...

        // Obtenemos el ID del precio actual (por si cambió de plan en el portal)
        String newPriceId = subscription.getItems().getData().get(0).getPrice().getId();

        System.out.println("🔄 Suscripción Actualizada: " + stripeCustomerId + " -> " + status);

        // Actualizamos la DB
        authService.updateSubscriptionStatus(stripeCustomerId, status, newPriceId);
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        String stripeCustomerId = subscription.getCustomer();

        System.out.println("❌ Suscripción Cancelada Definitivamente: " + stripeCustomerId);

        // Actualizamos la DB a 'canceled' y removemos el plan
        authService.updateSubscriptionStatus(stripeCustomerId, "canceled", null);
    }

    private void handleInvoicePaymentSucceeded(Invoice invoice) {
        // Este evento ocurre cada mes cuando se cobra la suscripción automáticamente
        if (invoice.getAmountPaid() == 0) return; // Ignoramos facturas de prueba o monto 0

        Payment payment = new Payment();
        payment.setStripeSessionId(invoice.getId()); // Usamos el ID de la factura como referencia
        payment.setAmount(invoice.getAmountPaid() / 100.0);
        payment.setCurrency(invoice.getCurrency().toUpperCase());
        payment.setCustomerEmail(invoice.getCustomerEmail());
        payment.setStatus("succeeded");
        payment.setDate(LocalDateTime.now());

        paymentRepository.save(payment);
        System.out.println("💰 Pago recurrente registrado: " + invoice.getCustomerEmail());
    }
}
