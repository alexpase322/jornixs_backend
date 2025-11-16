package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.RegistrationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegistrationInvitationRepository extends JpaRepository<RegistrationInvitation, Long> {
    Optional<RegistrationInvitation> findByToken(String token);
}