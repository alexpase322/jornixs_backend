package com.apv.chronotrack.utils;
 // Puedes ponerlo en el paquete 'config' o uno nuevo como 'util'

import com.apv.chronotrack.models.Role;
import com.apv.chronotrack.models.RoleName;
import com.apv.chronotrack.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Llama al método para cargar los roles
        loadRoles();
    }

    private void loadRoles() {
        // Revisa si el rol de trabajador ya existe. Si no, lo crea.
        if (roleRepository.findByRoleName(RoleName.ROLE_TRABAJADOR).isEmpty()) {
            Role workerRole = new Role();
            workerRole.setRoleName(RoleName.ROLE_TRABAJADOR);
            roleRepository.save(workerRole);
            log.info("WORKER role created.");
        }

        // Revisa si el rol de administrador ya existe. Si no, lo crea.
        if (roleRepository.findByRoleName(RoleName.ROLE_ADMINISTRADOR).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setRoleName(RoleName.ROLE_ADMINISTRADOR);
            roleRepository.save(adminRole);
            log.info("ADMINISTRATOR role created.");
        }
    }
}
