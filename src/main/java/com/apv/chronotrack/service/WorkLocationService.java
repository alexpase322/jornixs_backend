package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.CreateOrUpdateLocationRequest;
import com.apv.chronotrack.DTO.GeocodeAddressResponse;
import com.apv.chronotrack.DTO.WorkLocationDto;
import com.apv.chronotrack.models.Company;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.models.WorkLocation;
import com.apv.chronotrack.repository.UserRepository;
import com.apv.chronotrack.repository.WorkLocationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkLocationService {

    private final WorkLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final GeocodingService geocodingService;

    @Transactional
    public WorkLocationDto createLocation(CreateOrUpdateLocationRequest request, User admin) {
        User freshAdmin = findFreshUser(admin);
        Company company = freshAdmin.getCompany();

        WorkLocation newLocation = new WorkLocation();
        newLocation.setName(request.getName());
        newLocation.setAddress(request.getAddress());
        GeocodingService.GeocodingResult coordinates = resolveCoordinates(request);
        newLocation.setLatitude(coordinates.latitude());
        newLocation.setLongitude(coordinates.longitude());
        newLocation.setGeofenceRadiusMeters(request.getGeofenceRadiusMeters());
        newLocation.setCompany(company);

        WorkLocation savedLocation = locationRepository.save(newLocation);
        auditService.logAction(
                admin,
                admin.getCompany(),
                "WORK_LOCATION_CREATED",
                WorkLocation.class.getSimpleName(),
                savedLocation.getId(),
                "Work location created: " + savedLocation.getName()
        );
        return convertToDto(savedLocation);
    }

    @Transactional(readOnly = true)
    public List<WorkLocationDto> getLocationsByCompany(User admin) {
        User freshAdmin = findFreshUser(admin);
        return locationRepository.findByCompanyId(freshAdmin.getCompany().getId())
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkLocationDto updateLocation(Long locationId, CreateOrUpdateLocationRequest request, User admin) {
        User freshAdmin = findFreshUser(admin);
        WorkLocation location = findLocationAndVerifyCompany(locationId, freshAdmin);
        GeocodingService.GeocodingResult coordinates = resolveCoordinates(request);

        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setLatitude(coordinates.latitude());
        location.setLongitude(coordinates.longitude());
        location.setGeofenceRadiusMeters(request.getGeofenceRadiusMeters());

        WorkLocation updatedLocation = locationRepository.save(location);
        auditService.logAction(
                admin,
                admin.getCompany(),
                "WORK_LOCATION_UPDATED",
                WorkLocation.class.getSimpleName(),
                location.getId(),
                "Work location updated: " + location.getName()
        );
        return convertToDto(updatedLocation);
    }

    @Transactional
    public void deleteLocation(Long locationId, User admin) {
        User freshAdmin = findFreshUser(admin);
        WorkLocation location = findLocationAndVerifyCompany(locationId, freshAdmin);
        // Aquí podrías añadir lógica para reasignar trabajadores antes de borrar
        auditService.logAction(
                admin,
                admin.getCompany(),
                "WORK_LOCATION_DELETED",
                WorkLocation.class.getSimpleName(),
                location.getId(),
                "Work location deleted: " + location.getName()
        );
        locationRepository.delete(location);
    }

    // --- Métodos Privados Auxiliares ---
    private User findFreshUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }

    private WorkLocation findLocationAndVerifyCompany(Long locationId, User admin) {
        WorkLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Work location not found with ID: " + locationId));

        if (!location.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new SecurityException("Access denied: this work location does not belong to your company.");
        }
        return location;
    }

    private WorkLocationDto convertToDto(WorkLocation location) {
        return WorkLocationDto.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .geofenceRadiusMeters(location.getGeofenceRadiusMeters())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkLocationDto getLocationById(Long locationId, User admin) {
        User freshAdmin = findFreshUser(admin);
        WorkLocation location = findLocationAndVerifyCompany(locationId, freshAdmin);
        return convertToDto(location);
    }

    @Transactional(readOnly = true)
    public GeocodeAddressResponse geocodeAddress(String address, User admin) {
        findFreshUser(admin);
        GeocodingService.GeocodingResult geocodingResult = geocodingService.geocodeAddress(address);
        return GeocodeAddressResponse.builder()
                .latitude(geocodingResult.latitude())
                .longitude(geocodingResult.longitude())
                .normalizedAddress(geocodingResult.normalizedAddress())
                .build();
    }

    private GeocodingService.GeocodingResult resolveCoordinates(CreateOrUpdateLocationRequest request) {
        boolean hasLatitude = request.getLatitude() != null;
        boolean hasLongitude = request.getLongitude() != null;

        if (hasLatitude && hasLongitude) {
            return new GeocodingService.GeocodingResult(request.getLatitude(), request.getLongitude(), request.getAddress());
        }

        if (hasLatitude != hasLongitude) {
            throw new IllegalArgumentException("You must provide both latitude and longitude together, or neither to geocode by address.");
        }

        return geocodingService.geocodeAddress(request.getAddress());
    }
}
