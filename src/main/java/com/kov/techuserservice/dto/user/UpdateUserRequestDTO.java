package com.kov.techuserservice.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса на обновление профиля пользователя.
 * Используется в {@code UserController} для частичного обновления данных.
 * Все поля опциональны — обновляются только переданные значения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDTO {

    /** Новое имя пользователя. Если передано — заменяет текущее значение в профиле. */
    private String firstName;

    /** Новая фамилия пользователя. Если передано — заменяет текущее значение в профиле. */
    private String lastName;

    /** Новый email пользователя. Должен быть уникальным в системе. */
    @Email
    private String email;

    /** Новый телефонный номер в международном формате. */
    private String phone;

    /** Текущий пароль — требуется для подтверждения личности при изменении данных. */
    @NotBlank
    private String currentPassword;

    /** Новый пароль пользователя. Минимум 6 символов. Заменяет текущий пароль после проверки {@code currentPassword}. */
    @Size(min = 6)
    private String newPassword;
}
