package com.apv.chronotrack.repository;


import com.apv.chronotrack.models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    // Busca todos los registros de un usuario en un rango de fechas, ordenados por tiempo.
    List<TimeLog> findByUserAndTimestampBetweenOrderByTimestampAsc(User user, LocalDateTime start, LocalDateTime end);

    // Busca el último registro de un usuario para validar el estado actual (trabajando, en almuerzo, etc.)
    Optional<TimeLog> findTopByUserOrderByTimestampDesc(User user);
    @Query("SELECT COUNT(DISTINCT tl.user) FROM TimeLog tl WHERE tl.user.company = :company AND tl.timestamp BETWEEN :start AND :end")
    long countActiveWorkersBetween(@Param("company") Company company, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Obtiene todos los registros de una compañía en un rango de fechas
    @Query("SELECT tl FROM TimeLog tl WHERE tl.user.company = :company AND tl.timestamp BETWEEN :start AND :end ORDER BY tl.user.id, tl.timestamp")
    List<TimeLog> findByCompanyAndTimestampBetween(@Param("company") Company company, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<TimeLog> findByUserAndWorkWeekOrderByTimestampAsc(User user, WorkWeek workWeek);

    boolean existsByUserAndEventTypeAndTimestampBetween(User user, EventType eventType, LocalDateTime start, LocalDateTime end);
}
