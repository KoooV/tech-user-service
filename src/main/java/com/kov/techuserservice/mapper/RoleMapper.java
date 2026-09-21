package com.kov.techuserservice.mapper;

import com.kov.techuserservice.dto.enums.RoleName;
import com.kov.techuserservice.dto.role.RoleDTO;
import com.kov.techuserservice.dto.role.RoleUpdateDTO;
import com.kov.techuserservice.entity.Permission;
import com.kov.techuserservice.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleMapper INSTANCE = Mappers.getMapper(RoleMapper.class);

    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "permissionsToNames")
    RoleDTO toDTO(Role role);

    Role toEntity(RoleUpdateDTO dto);

    @Named("permissionsToNames")
    default Set<String> permissionsToNames(Set<Permission> permissions) {
        if (permissions == null) {
            return null;
        }
        return permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}