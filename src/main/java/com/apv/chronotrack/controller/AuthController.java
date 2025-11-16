package com.apv.chronotrack.controller;

import com.apv.chronotrack.DTO.*;
import com.apv.chronotrack.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Endpoint público para que el empleado complete su registro
    @PostMapping("/complete-registration")
    public ResponseEntity<AuthResponse> completeRegistration(@Valid @RequestBody RegistrationCompletionRequest request) {
        return ResponseEntity.ok(authService.completeRegistration(request));
    }

    // Endpoint público para el login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Endpoint solo para administradores
    @PostMapping("/invite")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<String> inviteUser(@Valid @RequestBody InviteRequest request) {
        authService.inviteUser(request);
        return ResponseEntity.ok("Invitación enviada correctamente.");
    }

    @PostMapping("/register-company")
    public ResponseEntity<AuthResponse> registerCompany(@Valid @RequestBody CompanyRegistrationRequest request) {
        return ResponseEntity.ok(authService.registerCompanyAndAdmin(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> payload) {
        authService.requestPasswordReset(payload.get("email"));
        return ResponseEntity.ok("Si el correo electrónico existe en nuestro sistema, se ha enviado un enlace para restablecer la contraseña.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.performPasswordReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("La contraseña ha sido actualizada correctamente.");
    }
}
