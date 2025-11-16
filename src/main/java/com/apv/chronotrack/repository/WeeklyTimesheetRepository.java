package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WeeklyTimesheetRepository extends JpaRepository<WeeklyTimesheet, Long> {
    Optional<WeeklyTimesheet> findByUserAndWorkWeek(User user, WorkWeek workWeek);
    List<WeeklyTimesheet> findByUser_CompanyAndStatus(Company company, TimesheetStatus status);

    @Query("SELECT ts FROM WeeklyTimesheet ts WHERE ts.user.company = :company " +
            "AND (:status IS NULL OR ts.status = :status) " +
            "AND (:userId IS NULL OR ts.user.id = :userId)")
    List<WeeklyTimesheet> findFilteredTimesheets(
            @Param("company") Company company,
            @Param("status") TimesheetStatus status,
            @Param("userId") Long userId
    );
}