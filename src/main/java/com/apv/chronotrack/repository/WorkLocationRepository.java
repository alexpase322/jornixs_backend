package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.WorkLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkLocationRepository extends JpaRepository<WorkLocation, Long> {
    // Método para encontrar todos los lugares de trabajo de una compañía
    List<WorkLocation> findByCompanyId(Long companyId);
}