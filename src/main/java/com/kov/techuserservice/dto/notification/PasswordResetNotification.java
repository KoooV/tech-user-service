package com.kov.techuserservice.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Контракт события сброса пароля для notification-сервиса.
 *
 * <p>Текущая реализация — {@code LoggingNotificationService} (только лог, без секретов
 * в открытом виде). При появлении notification-сервиса / брокера заменить реализацию на
 * отправку этого DTO в очередь (RabbitMQ/Kafka) или Feign-вызов, не меняя
 * {@code UserService.resetPassword}: контракт уже зафиксирован здесь.
 *
 * <p>Поле {@code temporaryPassword} передаётся только по защищённому каналу и никогда
 * не пишется в логи и не возвращается в REST-ответах.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetNotification {

    /** ID пользователя, чей пароль сброшен. */
    private Long userId;

    /** Email получателя уведомления. */
    private String email;

    /** Временный пароль в открытом виде — только для notification-сервиса. */
    private String temporaryPassword;
}
