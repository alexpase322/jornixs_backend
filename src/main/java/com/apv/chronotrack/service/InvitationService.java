package com.apv.chronotrack.service;

import com.apv.chronotrack.models.RegistrationInvitation;
import com.apv.chronotrack.repository.RegistrationInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final RegistrationInvitationRepository invitationRepository;
    private final EmailService emailService; // Reutilizamos el servicio de correo

    // Este método será llamado por el webhook de la pasarela de pagos
    public void createAndSendInvitation(String email, String planId, String customerId, String subscriptionId) {
        String token = UUID.randomUUID().toString();

        RegistrationInvitation invitation = new RegistrationInvitation();
        invitation.setEmail(email);
        invitation.setToken(token);
        invitation.setExpiryDate(LocalDateTime.now().plusDays(7));
        invitation.setPlanId(planId); // <-- Guardamos el plan
        invitation.setStripeCustomerId(customerId); // <-- Guardar
        invitation.setStripeSubscriptionId(subscriptionId); // <-- Guardar

        invitationRepository.save(invitation);
        emailService.sendCompanyRegistrationInvite(email, token);
    }
}
