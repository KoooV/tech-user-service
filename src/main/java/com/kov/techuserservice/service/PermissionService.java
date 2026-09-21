package com.kov.techuserservice.service;

import com.kov.techuserservice.entity.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionService {

    Optional<Permission> findByName(String name);

    List<Permission> findAll();

    Permission getOrCreate(String name, String description);
}
