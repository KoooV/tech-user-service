package com.kov.techuserservice.entity.repository;

import com.kov.techuserservice.dto.enums.RoleName;
import com.kov.techuserservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
