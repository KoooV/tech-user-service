package com.kov.techuserservice.service;

import com.kov.techuserservice.dto.enums.RoleName;
import com.kov.techuserservice.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {

    Optional<Role> findByName(RoleName name);

    List<Role> findAll();

    /**
     * Возвращает существующую роль или atomically создаёт новую.
     * Гарантирует, что register/assignRole никогда не оставят пользователя с пустым roles.
     */
    Role getOrCreate(RoleName name);

    Role getOrCreate(RoleName name, String description);

    /** Идемпотентно создаёт USER, MANAGER, ADMIN (fallback, если Flyway-сид ещё не применён). */
    void ensureDefaultRoles();
}
