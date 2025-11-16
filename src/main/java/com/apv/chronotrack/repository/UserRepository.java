package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.Company;
import com.apv.chronotrack.models.RoleName;
import com.apv.chronotrack.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA creará automáticamente la consulta para buscar un usuario por su email.
    Optional<User> findByEmail(String email);
    Optional<User> findByRegistrationToken(String token);
    // Cuenta el total de trabajadores de una compañía
    long countByCompany(Company company);

    // Encuentra todos los trabajadores de una compañía
    List<User> findByCompany(Company company);

    // Busca usuarios por Compañía Y cuyo campo accountActive sea 'true'
    List<User> findByCompanyAndAccountActiveTrue(Company company);

    List<User> findByCompanyAndAccountActiveFalse(Company company);

    Optional<User> findByPasswordResetToken(String token);

    long countByCompanyAndRole_RoleName(Company company, RoleName roleName);
}
