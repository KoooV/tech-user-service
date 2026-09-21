package com.kov.techuserservice.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса на регистрацию нового пользователя.
 * Используется при создании учётной записи через {@code AuthController}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

    /** Имя пользователя. Отображаемое имя, используется в интерфейсе и уведомлениях. */
    @NotBlank
    private String firstName;

    /** Фамилия пользователя. Используется вместе с firstName для формирования полного имени. */
    @NotBlank
    private String lastName;

    /** Электронная почта пользователя. Уникальный идентификатор для входа и уведомлений. */
    @NotBlank
    @Email
    private String email;

    /** Пароль пользователя. Хранится в хешированном виде (BCrypt). Минимум 6 символов. */
    @NotBlank
    @Size(min = 6)
    private String password;

    /** Телефонный номер в международном формате (+7..., +1...). Используется для SMS-уведомлений и двухфакторной аутентификации. */
    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,15}$")
    private String phone;
}
