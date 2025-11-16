package com.apv.chronotrack.models;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails { // Implementamos UserDetails

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "address")
    private String address;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "is_active")
    private boolean accountActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_token_expiry_date")
    private LocalDateTime passwordResetTokenExpiry;


    private LocalDateTime termsAcceptanceDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("user-timelogs") // <-- AÑADIR
    private List<TimeLog> timeLogs;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonBackReference("company-users") // <-- AÑADIR
    private Company company;

    @Column(columnDefinition = "TEXT") // Columna para guardar el JSON del W-9
    private String w9Data;

    @Column(name = "registration_token")
    private String registrationToken;

    @Column(name = "token_expiry_date")
    private LocalDateTime tokenExpiryDate;

    // AÑADIR ESTO
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("user-assignments") // <-- AÑADIR
    private List<UserWorkLocationAssignment> workAssignments;

    // Métodos requeridos por la interfaz UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Devolvemos el rol del usuario como una autoridad para Spring Security.
        return List.of(new SimpleGrantedAuthority(role.getRoleName().name()));
    }

    @Override
    public String getPassword() {
        // Spring Security usará este método para obtener la contraseña hasheada.
        return passwordHash;
    }

    @Override
    public String getUsername() {
        // Usaremos el email como el "username" para la autenticación.
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Podríamos añadir lógica para que las cuentas expiren.
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Podríamos añadir lógica para bloquear cuentas.
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.accountActive; // Usamos nuestro campo 'isActive'.
    }
}