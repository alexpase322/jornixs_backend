package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.Role;
import com.apv.chronotrack.models.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(RoleName roleName);
}
