package com.kov.techuserservice.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса аутентификации (логина).
 * Передаётся в {@code AuthController} для получения JWT-токенов.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {

    /** Электронная почта пользователя, используемая как логин для идентификации. */
    @NotBlank
    @Email
    private String email;

    /** Пароль пользователя. Используется совместно с email для проверки подлинности через Spring Security. */
    @NotBlank
    @Size(min = 6)
    private String password;
}
