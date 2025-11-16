package com.apv.chronotrack.controller;

import com.apv.chronotrack.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final InvitationService invitationService;

    // Endpoint de prueba para generar una invitación
    /*
    @PostMapping("/generate-invite")
    public ResponseEntity<String> generateInvite(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("El email es requerido.");
        }
        invitationService.createAndSendInvitation(email);
        return ResponseEntity.ok("Invitación de prueba generada para: " + email + ". Revisa tu correo.");

    }
     */
}