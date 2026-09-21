package com.kov.techuserservice.mapper;

import com.kov.techuserservice.dto.*;
import com.kov.techuserservice.entity.Address;
import com.kov.techuserservice.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface UserMapper {

    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    User toEntity(UserRequestDTO request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "active", source = "active")
    UserResponseDTO toResponse(User user);

    void updateFromDto(UpdateUserRequestDTO dto, @MappingTarget User user);
}
