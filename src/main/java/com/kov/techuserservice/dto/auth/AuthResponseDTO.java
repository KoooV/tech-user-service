package com.kov.techuserservice.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа на запрос аутентификации.
 * Содержит пару JWT-токенов, необходимых для авторизованных запросов к API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    /** Короткоживущий JWT-токен доступа. Используется в заголовке {@code Authorization: Bearer <token>} для авторизации запросов. Срок действия ограничен. */
    private String accessToken;

    /** Долгоживущий JWT-токен обновления. Используется для получения нового {@code accessToken} без повторного ввода пароля. Хранится безопасно (HttpOnly cookie / secure storage). */
    private String refreshToken;

    /** Тип токена (обычно "Bearer"). Указывает способ использования токена в заголовке авторизации. */
    private String tokenType;

    /** Время жизни {@code accessToken} в миллисекундах. Клиент должен обновить токен до истечения этого срока. */
    private Long expiresIn;
}
