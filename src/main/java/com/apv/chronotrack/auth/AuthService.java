package com.apv.chronotrack.auth;

import com.apv.chronotrack.DTO.*;
import com.apv.chronotrack.models.*;
import com.apv.chronotrack.repository.*;
import com.apv.chronotrack.service.AuditService;
import com.apv.chronotrack.service.EmailService;
import com.apv.chronotrack.utils.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Dependencias inyectadas a través del constructor de Lombok
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final EmailService emailService;
    private final RegistrationInvitationRepository invitationRepository; // <-- Añadir dependencia
    private final WorkLocationRepository workLocationRepository; // <-- Añadir dependencia
    private final UserWorkAssignmentRepository assignmentRepository;
    private final AuditService auditService;
    /**
     * Inicia el proceso de registro de un nuevo trabajador.
     * Este método es llamado por un administrador.
     * @param request Contiene el email y la tarifa por hora del nuevo empleado.
     */
    @Transactional
    public void inviteUser(InviteRequest request) {
        // 1. Obtiene al administrador autenticado que realiza la acción.
        User admin = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Company adminCompany = admin.getCompany();
        long currentWorkerCount = userRepository.countByCompanyAndRole_RoleName(adminCompany, RoleName.ROLE_TRABAJADOR);
        int planLimit = getLimitForPlan(adminCompany.getSubscriptionPlan());

        if (currentWorkerCount >= planLimit) {
            throw new IllegalStateException("Has alcanzado el límite de " + planLimit + " trabajadores para tu plan '" + adminCompany.getSubscriptionPlan() + "'. Por favor, actualiza tu suscripción.");
        }
        // 2. Valida que el email no esté ya en uso.
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("El correo electrónico ya está registrado.");
        }

        // 3. Obtiene el rol por defecto para un nuevo empleado.
        Role workerRole = roleRepository.findByRoleName(RoleName.ROLE_TRABAJADOR)
                .orElseThrow(() -> new IllegalStateException("Rol TRABAJADOR no encontrado en la base de datos."));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(1);

        if (adminCompany.getSubscriptionStatus() == SubscriptionStatus.CANCELED) {
            // Aquí podrías añadir una lógica más compleja para ver si la fecha de fin del período ya pasó
            throw new IllegalStateException("Tu suscripción ha sido cancelada. No puedes añadir nuevos trabajadores.");
        }

        // 4. Crea la nueva entidad de usuario con un estado inactivo.
        var newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setHourlyRate(request.getHourlyRate());
        newUser.setCompany(adminCompany);
        newUser.setRole(workerRole);
        newUser.setPasswordHash("TEMPORAL_PASSWORD_PLACEHOLDER");
        newUser.setAccountActive(false);
        newUser.setRegistrationToken(token); // Guardamos el token
        newUser.setTokenExpiryDate(expiryDate); // La cuenta se activará cuando el usuario complete el registro.
        User savedUser = userRepository.save(newUser);
        if (request.getWorkLocationId() != null) {
            WorkLocation location = workLocationRepository.findById(request.getWorkLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("Lugar de trabajo no encontrado."));

            if (!location.getCompany().getId().equals(adminCompany.getId())) {
                throw new SecurityException("Acceso denegado: Lugar de trabajo inválido.");
            }

            // Creamos el nuevo registro de asignación
            UserWorkLocationAssignment assignment = new UserWorkLocationAssignment();
            assignment.setUser(savedUser);

            assignment.setWorkLocation(location);
            assignment.setCurrent(true); // Esta es su primera y actual asignación
            assignmentRepository.save(assignment);
        }
        emailService.sendRegistrationInvite(newUser.getEmail(), token, adminCompany.getCompanyName());
        auditService.logAction(
                admin,
                admin.getCompany(),
                "WORKER_INVITED",
                User.class.getSimpleName(),
                savedUser.getId(),
                "Se invitó al nuevo trabajador: " + savedUser.getEmail()
        );
        // 5. Simula el envío del correo electrónico de invitación.
        // En una aplicación real, aquí se integraría un servicio de email (como SendGrid, AWS SES, etc.)
        // para enviar un enlace único al nuevo usuario.
        System.out.println("Enviando email de invitación para completar el registro a: " + request.getEmail());
    }

    /**
     * Completa el registro de un trabajador que fue previamente invitado.
     * @param request Contiene todos los datos del perfil y del formulario W-9.
     * @return AuthResponse con el token JWT para el inicio de sesión automático.
     */
    @Transactional
    @SneakyThrows // Anotación de Lombok para manejar la excepción de objectMapper.writeValueAsString
    public AuthResponse completeRegistration(RegistrationCompletionRequest request) {
        // 1. Busca al usuario por el email proporcionado en la invitación.
        User user = userRepository.findByRegistrationToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de registro inválido."));

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El token de registro ha expirado.");
        }
        // 2. Actualiza los datos de perfil y acceso.
        user.setRegistrationToken(null);
        user.setTokenExpiryDate(null);
        user.setAccountActive(true);
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAccountActive(true); // ¡Importante! Activa la cuenta.

        // 3. Estructura la información del formulario W-9 en un objeto DTO.
        var w9Info = new W9Data(
                request.getBusinessName(),
                request.getTaxClassification(),
                request.getExemptPayeeCode(),
                request.getFatcaExemptionCode(),
                request.getStreetAddress(),
                request.getCityStateZip(),
                request.getSsn(),
                request.getEin()
        );

        // 4. Convierte el objeto W9Data a un String en formato JSON y lo guarda.
        String w9Json = objectMapper.writeValueAsString(w9Info);
        user.setW9Data(w9Json);

        // 5. Guarda el usuario completamente actualizado en la base de datos.
        userRepository.save(user);

        // 6. Genera un token JWT para que el usuario inicie sesión inmediatamente.
        String jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder().token(jwtToken).build();
    }

    /**
     * Autentica a un usuario existente y devuelve un token JWT.
     * @param request Contiene el email y la contraseña del usuario.
     * @return AuthResponse con el token JWT.
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Valida las credenciales contra la base de datos.
        // Si las credenciales son incorrectas, aquí se lanzará una excepción.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Si la autenticación es exitosa, busca al usuario.
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        // 3. Genera y devuelve el token JWT.
        String jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder().token(jwtToken).build();
    }
    /**
     * Registra una nueva compañía y su primer usuario administrador.
     * Esta operación es atómica gracias a @Transactional.
     * @param request Contiene los datos de la compañía y del administrador.
     * @return AuthResponse con el token JWT para el nuevo administrador.
     */
    @Transactional
    public AuthResponse registerCompanyAndAdmin(CompanyRegistrationRequest request) {
        RegistrationInvitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("El token de invitación es inválido."));

        if (invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El token de invitación ha expirado.");
        }

        if (!invitation.getEmail().equalsIgnoreCase(request.getAdminEmail())) {
            throw new SecurityException("El correo electrónico no coincide con el de la invitación.");
        }
        // 1. Validar que la compañía (por EIN) y el admin (por email) no existan.
        if (companyRepository.findByEin(request.getEin()).isPresent()) {
            throw new IllegalStateException("Una compañía con este EIN ya está registrada.");
        }
        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new IllegalStateException("Un usuario con este correo electrónico ya está registrado.");
        }

        String planId = invitation.getPlanId();
        String planName = getPlanNameFromPriceId(planId); // <-- Usaremos un método auxiliar

        // 2. Crear y guardar la nueva compañía.
        Company newCompany = new Company();
        newCompany.setCompanyName(request.getCompanyName());
        newCompany.setAddress(request.getCompanyAddress());
        newCompany.setPhoneNumber(request.getCompanyPhoneNumber());
        newCompany.setEin(request.getEin());
        newCompany.setSubscriptionPlan(planName);
        newCompany.setWorkLatitude(request.getWorkLatitude());
        newCompany.setWorkLongitude(request.getWorkLongitude());
        newCompany.setGeofenceRadiusMeters(request.getGeofenceRadiusMeters());
        newCompany.setStripeCustomerId(invitation.getStripeCustomerId());
        newCompany.setStripeSubscriptionId(invitation.getStripeSubscriptionId());
        newCompany.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        newCompany.setSubscriptionPlan(planName); // <-- Guardamos el nombre del plan
        Company savedCompany = companyRepository.save(newCompany);

        // 3. Obtener el rol de Administrador.
        Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException("Rol ADMINISTRADOR no encontrado."));

        // 4. Crear el usuario administrador y asociarlo a la nueva compañía.
        User adminUser = new User();
        adminUser.setFullName(request.getAdminFullName());
        adminUser.setEmail(request.getAdminEmail());
        adminUser.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
        adminUser.setCompany(savedCompany); // ¡Vinculación clave!
        adminUser.setRole(adminRole);
        //adminUser.setTermsAccepted(true);
        adminUser.setTermsAcceptanceDate(LocalDateTime.now());
        adminUser.setAccountActive(true);
        // El valor por hora no aplica para el admin, podría ser 0 o nulo.
        adminUser.setHourlyRate(BigDecimal.ZERO);
        invitationRepository.delete(invitation);
        userRepository.save(adminUser);

        // 5. Generar y devolver el token JWT para el nuevo administrador.
        String jwtToken = jwtService.generateToken(adminUser);
        return AuthResponse.builder().token(jwtToken).build();
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró un usuario con ese correo electrónico."));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1)); // El token es válido por 1 hora

        userRepository.save(user);
        emailService.sendPasswordResetEmail(email, token);
    }

    @Transactional
    public void performPasswordReset(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token) // <-- Necesitaremos añadir este método al repositorio
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o no encontrado."));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El token de restablecimiento de contraseña ha expirado.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Limpiamos los tokens para que no se puedan reutilizar
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
    }

    private String getPlanNameFromPriceId(String priceId) {
        // Aquí mapeas los IDs de precio de Stripe a los nombres de tus planes
        // ¡IMPORTANTE! Estos IDs deben coincidir con los que tienes en tu dashboard de Stripe
        // y en el frontend.
        switch (priceId) {
            case "price_emprendedor_id":
                return "Emprendedor";
            case "price_crecimiento_id":
                return "Crecimiento";
            case "price_corporativo_id":
                return "Corporativo";
            default:
                throw new IllegalArgumentException("ID de precio no reconocido: " + priceId);
        }
    }

    private int getLimitForPlan(String planName) {
        switch (planName) {
            case "Emprendedor":
                return 10;
            case "Crecimiento":
                return 50;
            case "Corporativo":
                return Integer.MAX_VALUE; // O un número muy grande para "ilimitado"
            default:
                return 0; // Por defecto, no permite crear si el plan no es reconocido
        }
    }

    @Transactional
    public void updateSubscriptionStatus(String stripeCustomerId, String status, String newPriceId) {
        // 1. Buscar la compañía por el ID de cliente de Stripe (cus_...)
        Company company = companyRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseThrow(() -> new RuntimeException("⚠️ Webhook Error: No se encontró compañía con Stripe ID: " + stripeCustomerId));

        // 2. Actualizar el estado
        // Mapeamos el string de Stripe a tu Enum o String local
        company.setSubscriptionStatus(mapStripeStatus(status));

        // 3. Si hubo cambio de plan (Upgrade/Downgrade), actualizamos la info del plan
        if (newPriceId != null && !newPriceId.equals(company.getPlanPriceId())) {
            company.setPlanPriceId(newPriceId);

            // Opcional: Aquí podrías llamar a un método para actualizar límites
            // updateCompanyLimitsBasedOnPlan(company, newPriceId);
            System.out.println("Plan actualizado para la empresa: " + company.getCompanyName());
        }

        companyRepository.save(company);
    }

    // Helper para convertir el status de Stripe a tu formato
    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) return SubscriptionStatus.INACTIVE;

        switch (stripeStatus.toLowerCase()) {
            case "active":
                return SubscriptionStatus.ACTIVE;
            case "trialing":
                return SubscriptionStatus.TRIALING; // O ACTIVE si prefieres tratarlos igual
            case "canceled":
                return SubscriptionStatus.CANCELED;
            case "past_due":
            case "unpaid":
                return SubscriptionStatus.PAST_DUE;
            default:
                return SubscriptionStatus.INACTIVE;
        }
    }
}