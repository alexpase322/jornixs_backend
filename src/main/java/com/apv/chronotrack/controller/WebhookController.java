package com.apv.chronotrack.controller;

import com.apv.chronotrack.DTO.StripeWebhookEvent;
import com.apv.chronotrack.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final InvitationService invitationService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody StripeWebhookEvent event) {
        if ("checkout.session.completed".equals(event.getType())) {
            Map<String, Object> sessionData = (Map<String, Object>) event.getData().getObject();
            Map<String, String> customerDetails = (Map<String, String>) sessionData.get("customer_details");

            // --- LÓGICA CORREGIDA ---
            String clientReferenceId = (String) sessionData.get("client_reference_id"); // <-- Obtenemos el planId
            String customerEmail = customerDetails.get("email");
            String customerId = (String) sessionData.get("customer"); // <-- Nuevo
            String subscriptionId = (String) sessionData.get("subscription");

            if (customerEmail != null && clientReferenceId != null) {
                System.out.println("Pago exitoso para el plan: " + clientReferenceId + " por el cliente: " + customerEmail);
                invitationService.createAndSendInvitation(customerEmail, clientReferenceId, customerId, subscriptionId);
            }
        }
        return ResponseEntity.ok("Webhook recibido");
    }
}
