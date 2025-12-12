package com.apv.chronotrack.controller;

import com.apv.chronotrack.models.Company;
import com.apv.chronotrack.models.SubscriptionStatus;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.repository.CompanyRepository;
import com.apv.chronotrack.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Value("${stripe.api.secret-key}")
    private String stripeSecretKey;

    @Value("${frontend.base.url}")
    private String frontendUrl;

    public PaymentController(UserRepository userRepository, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CheckoutRequest request) throws StripeException {

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl)
                .setClientReferenceId(request.getPriceId()) // Tu ID de plan

                // --- AQUI ESTÁ LA MAGIA DE LOS 14 DÍAS GRATIS ---
                .setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .setTrialPeriodDays(14L) // <--- ESTO ACTIVA EL TRIAL
                                .build()
                )

                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(request.getPriceId())
                                .setQuantity(1L)
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    // DTO simple para recibir el ID del precio desde el frontend
    @Data
    private static class CheckoutRequest {
        private String priceId;
    }

    // ... (imports de Stripe)
    @PostMapping("/cancel-subscription")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<String> cancelSubscription(@AuthenticationPrincipal User admin) throws StripeException {

        // Recargamos el admin y su compañía para asegurar que tenemos los datos
        User freshAdmin = userRepository.findById(admin.getId()).orElseThrow();
        Company company = freshAdmin.getCompany();

        if (company.getStripeSubscriptionId() == null) {
            throw new IllegalStateException("No se encontró información de la suscripción.");
        }

        // 1. Decirle a Stripe que cancele la suscripción al final del período
        Subscription subscription = Subscription.retrieve(company.getStripeSubscriptionId());
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();
        subscription.update(params);

        // 2. Actualizar nuestro estado interno
        company.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        companyRepository.save(company);

        return ResponseEntity.ok("Tu suscripción ha sido cancelada y permanecerá activa hasta el final de tu ciclo de facturación.");
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionDetails(@PathVariable String sessionId) {
        try {
            // 1. Recuperamos la sesión directamente de Stripe
            Session session = Session.retrieve(sessionId);

            // 2. Extraemos el monto (Stripe lo da en centavos, ej: 1000 = 10.00)
            Double amount = session.getAmountTotal() != null ? session.getAmountTotal() / 100.0 : 0.0;
            String currency = session.getCurrency() != null ? session.getCurrency().toUpperCase() : "USD";
            String status = session.getPaymentStatus();

            // 3. Devolvemos la respuesta limpia al Frontend
            Map<String, Object> response = new HashMap<>();
            response.put("amount", amount);
            response.put("currency", currency);
            response.put("status", status);
            response.put("customer_email", session.getCustomerDetails().getEmail());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create-portal-session")
    public ResponseEntity<Map<String, String>> createPortalSession(@AuthenticationPrincipal User user) throws StripeException {
        // 1. Obtenemos la compañía del usuario actual
        User freshUser = userRepository.findById(user.getId()).orElseThrow();
        Company company = freshUser.getCompany();

        // VALIDACIÓN: Necesitamos el Customer ID de Stripe (lo guardamos en el primer pago)
        if (company.getStripeCustomerId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No tienes una suscripción activa vinculada."));
        }

        // 2. Configuramos la sesión del portal
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(company.getStripeCustomerId()) // El ID del cliente (cus_...)
                .setReturnUrl(frontendUrl + "/worker/dashboard") // A donde vuelven al salir
                .build();

        // 3. Generamos el enlace
        Session session = Session.create(params);

        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }
}