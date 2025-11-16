package com.apv.chronotrack.controller;

import com.apv.chronotrack.DTO.UpdateProfileRequestDto;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Endpoint para que el usuario autenticado actualice su información de perfil.
     */
    @PutMapping("/update")
    public ResponseEntity<String> updateUserProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequestDto request) {

        profileService.updateUserProfile(user, request);
        return ResponseEntity.ok("Perfil actualizado correctamente.");
    }

    @GetMapping
    public ResponseEntity<UpdateProfileRequestDto> getUserProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getUserProfile(user));
    }
}
