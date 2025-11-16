package com.apv.chronotrack.repository; // O tu paquete

import com.apv.chronotrack.models.User;
import com.apv.chronotrack.models.UserWorkLocationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserWorkAssignmentRepository extends JpaRepository<UserWorkLocationAssignment, Long> {
    // Busca la asignación activa actual para un usuario
    Optional<UserWorkLocationAssignment> findByUserAndIsCurrentTrue(User user);
}
