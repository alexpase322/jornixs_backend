package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.UpdateProfileRequestDto;
import com.apv.chronotrack.DTO.W9Data;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Transactional
    @SneakyThrows
    public void updateUserProfile(User user, UpdateProfileRequestDto request) {
        // Actualizar campos directos si se proporcionaron
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getNewPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        // Actualizar datos dentro del JSON del W-9
        String w9Json = user.getW9Data();
        W9Data w9Data = (w9Json != null && !w9Json.isEmpty())
                ? objectMapper.readValue(w9Json, W9Data.class)
                : new W9Data(null, null, null, null, null, null, null, null);

        W9Data updatedW9Data = new W9Data(
                w9Data.businessName(), w9Data.taxClassification(), w9Data.exemptPayeeCode(),
                w9Data.fatcaExemptionCode(),
                StringUtils.hasText(request.getStreetAddress()) ? request.getStreetAddress() : w9Data.streetAddress(),
                StringUtils.hasText(request.getCityStateZip()) ? request.getCityStateZip() : w9Data.cityStateZip(),
                StringUtils.hasText(request.getSsn()) ? request.getSsn() : w9Data.ssn(),
                w9Data.ein()
        );

        user.setW9Data(objectMapper.writeValueAsString(updatedW9Data));
        userRepository.save(user);
        auditService.logAction(
                user,
                user.getCompany(),
                "USER_PASSWORD_CHANGED",
                User.class.getSimpleName(),
                user.getId(),
                "El usuario cambió su propia contraseña."
        );
    }

    @SneakyThrows
    public UpdateProfileRequestDto getUserProfile(User user) {
        W9Data w9Data = new W9Data(null, null, null, null, null, null, null, null);
        if (user.getW9Data() != null && !user.getW9Data().isEmpty()) {
            w9Data = objectMapper.readValue(user.getW9Data(), W9Data.class);
        }

        return UpdateProfileRequestDto.builder()
                .fullName(user.getFullName())
                .streetAddress(w9Data.streetAddress())
                .cityStateZip(w9Data.cityStateZip())
                .ssn(w9Data.ssn())
                .build();
    }


}