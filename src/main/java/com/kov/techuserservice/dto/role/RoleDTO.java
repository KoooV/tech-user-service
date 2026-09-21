package com.kov.techuserservice.dto.role;

import com.kov.techuserservice.dto.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO для представления роли пользователя с набором разрешений.
 * Определяет уровень доступа к административным и функциональным возможностям системы.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {

    /** Уникальный идентификатор роли в базе данных. */
    private Long id;

    /** Название роли. Определяет уровень доступа: USER — базовый доступ, MANAGER — управление заказами/товарами, ADMIN — полный контроль над системой. */
    @NotBlank
    private RoleName name;

    /** Множество строковых разрешений (permissions), связанных с ролью. Определяют конкретные действия, которые может выполнять пользователь с данной ролью (например: "ORDER_READ", "USER_DELETE"). */
    private Set<String> permissions;
}
