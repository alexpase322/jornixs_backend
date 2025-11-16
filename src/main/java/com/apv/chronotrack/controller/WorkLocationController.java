package com.apv.chronotrack.controller;

import com.apv.chronotrack.DTO.CreateOrUpdateLocationRequest;
import com.apv.chronotrack.DTO.WorkLocationDto;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.service.WorkLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/work-locations")
@RequiredArgsConstructor
public class WorkLocationController {

    private final WorkLocationService locationService;

    @PostMapping
    public ResponseEntity<WorkLocationDto> createLocation(@Valid @RequestBody CreateOrUpdateLocationRequest request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(locationService.createLocation(request, admin));
    }

    @GetMapping
    public ResponseEntity<List<WorkLocationDto>> getLocations(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(locationService.getLocationsByCompany(admin));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkLocationDto> updateLocation(@PathVariable Long id, @Valid @RequestBody CreateOrUpdateLocationRequest request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(locationService.updateLocation(id, request, admin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        locationService.deleteLocation(id, admin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkLocationDto> getLocationById(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(locationService.getLocationById(id, admin));
    }
}