package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.Company;
import com.apv.chronotrack.models.WorkWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface WorkWeekRepository extends JpaRepository<WorkWeek, Long> {
    // Busca una semana específica por su rango de fechas y compañía
    Optional<WorkWeek> findByCompanyAndStartDateAndEndDate(Company company, LocalDate startDate, LocalDate endDate);

    // Busca semanas dentro de un rango para una compañía
    List<WorkWeek> findByCompanyAndStartDateBetweenOrderByStartDateAsc(Company company, LocalDate start, LocalDate end);

    @Query("SELECT w FROM WorkWeek w WHERE w.company = :company AND w.startDate <= :endDate AND w.endDate >= :startDate ORDER BY w.startDate")
    List<WorkWeek> findOverlappingWeeks(
            @Param("company") Company company,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}