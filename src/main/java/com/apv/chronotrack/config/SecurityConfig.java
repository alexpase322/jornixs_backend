package com.apv.chronotrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Estos son los componentes que necesitaremos crear después.
    // Los inyectamos aquí para usarlos en la configuración.
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Las rutas para autenticación (login, registro) son públicas.
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/reports/**").authenticated()
                        // Rutas específicas para el administrador.
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMINISTRADOR")
                        // Rutas específicas para el trabajador.
                        .requestMatchers("/api/worker/**").hasAuthority("ROLE_TRABAJADOR")
                        .requestMatchers("/api/profile/**").hasAuthority("ROLE_TRABAJADOR")
                        .requestMatchers("/api/test/**").permitAll() // <-- Añade esta línea
                        .requestMatchers("/api/webhooks/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/webhooks/**", "/api/test/**").permitAll()
                        .requestMatchers("/api/payments/**").permitAll() // <-- AÑADE ESTA LÍNEA
                        // Cualquier otra solicitud debe ser autenticada.
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authenticationProvider(authenticationProvider)

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Define de qué orígenes (frontends) aceptas peticiones.
        // ¡Importante! Usa la URL de tu frontend de Angular.
        configuration.setAllowedOrigins(List.of("https://www.jornixs.com"));

        // Define qué métodos HTTP permites (GET, POST, etc.)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Define qué cabeceras permites.
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // Permite que las credenciales (como cookies o tokens) se incluyan.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta configuración a todas las rutas de tu API.
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
