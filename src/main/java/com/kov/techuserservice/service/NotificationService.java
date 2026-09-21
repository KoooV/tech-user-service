package com.kov.techuserservice.service;

import com.kov.techuserservice.dto.notification.PasswordResetNotification;

/**
 * Порт отправки пользовательских уведомлений (зона notification-сервиса).
 * Контракт зафиксирован здесь, чтобы {@code UserService.resetPassword} не был заглушкой:
 * сброс пароля всегда завершается уведомлением пользователя.
 */
public interface NotificationService {

    void sendPasswordReset(PasswordResetNotification notification);
}
