package com.kov.techuserservice.mapper;

import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.dto.role.RoleDTO;
import com.kov.techuserservice.dto.user.UpdateUserRequestDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;
import com.kov.techuserservice.entity.User;
import org.mapstruct.*;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования между сущностью {@link User} и DTO.
 * Используется для разделения слоя данных (Entity) и слоя представления (DTO).
 * MapStruct генерирует реализацию на этапе компиляции.
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface UserMapper {

    /**
     * Преобразует DTO регистрации в сущность User.
     * Поля roles, addresses, active, createdAt, updatedAt, password игнорируются —
     * они устанавливаются отдельно (роли назначаются при регистрации, пароль хешируется).
     *
     * @param request DTO регистрации пользователя
     * @return Сущность User для сохранения в БД
     */
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    User toEntity(UserRequestDTO request);

    /**
     * Преобразует сущность User в DTO ответа клиенту.
     * Использует ignoreByDefault и явное маппинг полей для корректной работы с @Builder.
     * Поля password, roles, addresses не включаются в ответ (безопасность).
     *
     * @param user Сущность User из БД
     * @return DTO для отправки клиенту
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "role", source = "roles", qualifiedByName = "firstRole")
    @Mapping(target = "addresses", source = "addresses")
    UserResponseDTO toResponse(User user);

    /**
     * Обновляет существующую сущность User данными из DTO обновления.
     * Применяет только переданные (непустые) поля к целевой сущности.
     *
     * @param dto DTO с новыми данными
     * @param user Сущность User для обновления (модифицируется in-place)
     */
    void updateFromDto(UpdateUserRequestDTO dto, @MappingTarget User user);

    @Named("firstRole")
    default RoleDTO firstRole(Set<com.kov.techuserservice.entity.Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        com.kov.techuserservice.entity.Role role = roles.iterator().next();
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        if (role.getPermissions() != null) {
            dto.setPermissions(role.getPermissions().stream()
                    .map(com.kov.techuserservice.entity.Permission::getName)
                    .collect(Collectors.toSet()));
        }
        return dto;
    }
}
