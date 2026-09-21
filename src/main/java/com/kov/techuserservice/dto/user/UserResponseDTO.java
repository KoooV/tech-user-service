package com.kov.techuserservice.dto.user;

import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.dto.role.RoleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для ответа клиенту с данными профиля пользователя.
 * Возвращается в ответах {@code UserController} и {@code AuthController}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    /** Уникальный идентификатор пользователя в базе данных (первичный ключ). */
    private Long id;

    /** Имя пользователя. */
    private String firstName;

    /** Фамилия пользователя. */
    private String lastName;

    /** Электронная почта — логин пользователя. */
    private String email;

    /** Телефонный номер пользователя. */
    private String phone;

    /** Статус активности аккаунта. {@code true} — аккаунт активен, {@code false} — заблокирован/деактивирован. */
    private boolean active;

    /** Роль пользователя (USER, MANAGER, ADMIN), определяющая набор прав доступа к функционалу системы. */
    private RoleDTO role;

    /** Список адресов доставки пользователя. Используется при оформлении заказа для выбора адреса получения. */
    private List<AddressDTO> addresses;
}
