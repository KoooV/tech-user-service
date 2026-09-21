package com.kov.techuserservice.exception;

/**
 * Email уже занят другим пользователем.
 * Отдельный тип (а не {@link SecurityException}), чтобы вернуть {@code 409 Conflict},
 * а не {@code 401 Unauthorized}.
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
