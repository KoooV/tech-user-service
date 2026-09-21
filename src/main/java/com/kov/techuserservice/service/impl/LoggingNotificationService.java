package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.notification.PasswordResetNotification;
import com.kov.techuserservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Временная реализация: только лог без секретов.
 * Заменить на MQ/Feign-отправку в notification-сервис, когда он появится
 * (сигнатура {@link NotificationService} при этом не меняется).
 */
@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {

    @Override
    public void sendPasswordReset(PasswordResetNotification notification) {
        // Никогда не логируем temporaryPassword.
        log.info("Password reset notification queued for user {} <{}>",
                notification.getUserId(), notification.getEmail());
    }
}
