package com.apv.chronotrack.controller;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.model.StripeObject;
import com.apv.chronotrack.DTO.StripeWebhookEvent;
import com.apv.chronotrack.service.InvitationService;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final InvitationService invitationService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload, // <--- 1. RECIBIMOS EL TEXTO CRUDO
            @RequestHeader("Stripe-Signature") String sigHeader // <--- 2. RECIBIMOS LA FIRMA
    ) {
        Event event;

        try {
            // 3. LA LIBRERÍA DE STRIPE HACE LA MAGIA (Valida y convierte)
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            System.err.println("⚠️ Error verificando firma del webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error de firma");
        }

        // 4. VERIFICAMOS EL TIPO DE EVENTO
        if ("checkout.session.completed".equals(event.getType())) {

            // 5. DESERIALIZAMOS DE FORMA SEGURA AL OBJETO 'SESSION'
            // Esto evita los errores de casting de Map
            Optional<StripeObject> objectOptional = event.getDataObjectDeserializer().getObject();

            if (objectOptional.isPresent() && objectOptional.get() instanceof Session) {
                Session session = (Session) objectOptional.get();

                // 6. EXTRAEMOS LOS DATOS USANDO LOS MÉTODOS OFICIALES
                String clientReferenceId = session.getClientReferenceId(); // Tu PriceID
                String customerEmail = session.getCustomerDetails().getEmail();
                String customerId = session.getCustomer();
                String subscriptionId = session.getSubscription();

                System.out.println("✅ Webhook procesado. Plan: " + clientReferenceId + ", Email: " + customerEmail);

                if (customerEmail != null && clientReferenceId != null) {
                    try {
                        invitationService.createAndSendInvitation(customerEmail, clientReferenceId, customerId, subscriptionId);
                    } catch (Exception e) {
                        System.err.println("❌ Error enviando invitación: " + e.getMessage());
                        // No devolvemos error 500 para que Stripe no reintente infinitamente si es un error lógico nuestro
                    }
                }
            }
        }

        return ResponseEntity.ok("Recibido");
    }
}
