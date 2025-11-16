package com.apv.chronotrack.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "work_weeks")
public class WorkWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Lunes de la semana

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // Domingo de la semana

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Puedes añadir un campo para el año o un identificador de semana si lo necesitas más adelante
    @Column(name = "week_number_of_year")
    private int weekNumberOfYear;
}