package com.apv.chronotrack.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_work_assignments")
public class UserWorkLocationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-assignments") // <-- AÑADIR
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_location_id", nullable = false)
    @JsonBackReference("location-assignments")
    private WorkLocation workLocation;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    @Column(name = "assignment_date", nullable = false)
    private LocalDateTime assignmentDate;

    @PrePersist
    public void onPrePersist() {
        assignmentDate = LocalDateTime.now();
    }
}