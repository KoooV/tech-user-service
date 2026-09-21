package com.kov.techuserservice.mapper;

import com.kov.techuserservice.dto.user.UpdateUserRequestDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;
import com.kov.techuserservice.entity.User;
import org.mapstruct.*;

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
    UserResponseDTO toResponse(User user);

    /**
     * Обновляет существующую сущность User данными из DTO обновления.
     * Применяет только переданные (непустые) поля к целевой сущности.
     *
     * @param dto DTO с новыми данными
     * @param user Сущность User для обновления (модифицируется in-place)
     */
    void updateFromDto(UpdateUserRequestDTO dto, @MappingTarget User user);
}
