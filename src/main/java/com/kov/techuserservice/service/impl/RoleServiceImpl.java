package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.enums.RoleName;
import com.kov.techuserservice.entity.Permission;
import com.kov.techuserservice.entity.Role;
import com.kov.techuserservice.entity.repository.PermissionRepository;
import com.kov.techuserservice.entity.repository.RoleRepository;
import com.kov.techuserservice.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private static final Map<RoleName, String> DEFAULT_DESCRIPTIONS = Map.of(
            RoleName.USER, "Regular user: catalog, own orders, addresses, profile",
            RoleName.MANAGER, "Manager: USER rights + orders of others, stats, inventory",
            RoleName.ADMIN, "Administrator: full control over users, roles, catalog, orders"
    );

    /** Базовые права для каждой роли (должны совпадать с V2__seed_roles_permissions.sql). */
    private static final Map<RoleName, Set<String>> DEFAULT_PERMISSIONS = Map.of(
            RoleName.USER, Set.of("USER_READ"),
            RoleName.MANAGER, Set.of("USER_READ", "USER_WRITE", "USER_ACTIVATE"),
            RoleName.ADMIN, Set.of("USER_READ", "USER_WRITE", "USER_DELETE", "ROLE_ASSIGN", "USER_ACTIVATE")
    );

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(RoleName name) {
        return roleRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public Role getOrCreate(RoleName name) {
        return getOrCreate(name, DEFAULT_DESCRIPTIONS.getOrDefault(name, name.name()));
    }

    @Override
    @Transactional
    public Role getOrCreate(RoleName name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Set<Permission> permissions = resolvePermissions(name);
                    Role created = roleRepository.save(Role.builder()
                            .name(name)
                            .description(description)
                            .permissions(permissions)
                            .build());
                    log.info("Role created: {}", name);
                    return created;
                });
    }

    @Override
    @Transactional
    public void ensureDefaultRoles() {
        for (RoleName name : RoleName.values()) {
            getOrCreate(name);
        }
    }

    private Set<Permission> resolvePermissions(RoleName roleName) {
        Set<String> names = DEFAULT_PERMISSIONS.getOrDefault(roleName, Set.of());
        Set<Permission> result = new HashSet<>();
        for (String permissionName : names) {
            Permission permission = permissionRepository.findByName(permissionName)
                    .orElseGet(() -> permissionRepository.save(Permission.builder()
                            .name(permissionName)
                            .description("Auto-created for role " + roleName)
                            .build()));
            result.add(permission);
        }
        return result;
    }
}
