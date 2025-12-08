package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Método útil para buscar si un pago ya existe (por si Stripe envía el evento dos veces)
    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    // Método para buscar todos los pagos de un email específico (útil para el historial del cliente)
    // List<Payment> findByCustomerEmail(String customerEmail);
}
