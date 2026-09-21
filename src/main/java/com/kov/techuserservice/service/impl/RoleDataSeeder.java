package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runtime-страховка: если Flyway-сид (V2) ещё не применён
 * (например, существующая БД до Flyway), дефолтные роли будут созданы.
 * Идемпотентно, выполняется при каждом старте.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class RoleDataSeeder implements ApplicationRunner {

    private final RoleService roleService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            roleService.ensureDefaultRoles();
            log.info("Default roles ensured (USER, MANAGER, ADMIN)");
        } catch (Exception e) {
            log.error("Failed to ensure default roles", e);
            throw e;
        }
    }
}
