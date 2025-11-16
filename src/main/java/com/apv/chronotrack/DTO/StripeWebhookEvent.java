package com.apv.chronotrack.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

// Este DTO mapea la estructura del webhook de Stripe
@Data
@NoArgsConstructor
public class StripeWebhookEvent {
    private Data data;
    private String type;

    @lombok.Data
    @NoArgsConstructor
    public static class Data {
        private Object object;
    }

    // Puedes crear una clase más específica si necesitas más datos del cliente
    // Por ahora, un Map genérico es suficiente para obtener el email
    // "object" contendrá los detalles de la sesión de pago
}