package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    // Método para validar que el EIN de la empresa no esté ya registrado
    Optional<Company> findByEin(String ein);
}