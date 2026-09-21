package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.enums.RoleName;
import com.kov.techuserservice.entity.Permission;
import com.kov.techuserservice.entity.Role;
import com.kov.techuserservice.entity.repository.PermissionRepository;
import com.kov.techuserservice.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> findByName(String name) {
        return permissionRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    @Override
    @Transactional
    public Permission getOrCreate(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    Permission created = permissionRepository.save(Permission.builder()
                            .name(name)
                            .description(description)
                            .build());
                    log.info("Permission created: {}", name);
                    return created;
                });
    }
}
