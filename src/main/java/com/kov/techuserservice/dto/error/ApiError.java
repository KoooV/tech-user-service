package com.kov.techuserservice.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Единый контракт тела ошибки для всего сервиса.
 * Все {@code 4xx/5xx} от REST-слоя (контроллеры, security entry points,
 * {@code GlobalExceptionHandler}) возвращают именно этот DTO, а не сырой {@code Map}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /** Момент формирования ошибки (UTC). */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /** HTTP-статус числом (400, 401, 403, 404, 409, 500). */
    private int status;

    /** Короткое имя статуса (BAD_REQUEST, UNAUTHORIZED, ...). */
    private String error;

    /** Человекочитаемое сообщение (одно, без plaintext-секретов). */
    private String message;

    /** Путь запроса, где произошла ошибка. */
    private String path;

    /**
     * Детализация по полям для ошибок валидации.
     * Ключ — имя поля/параметра, значение — сообщение.
     * {@code null}, когда детализация неприменима.
     */
    private Map<String, String> errors;
}
