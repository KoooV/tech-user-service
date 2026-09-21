package com.kov.techuserservice.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "Test security exception";
        SecurityException exception = new SecurityException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Test exception";
        Throwable cause = new RuntimeException("cause");
        SecurityException exception = new SecurityException(message);

        assertEquals(message, exception.getMessage());
    }
}

class UserNotFoundExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "User not found";
        UserNotFoundException exception = new UserNotFoundException(message);

        assertEquals(message, exception.getMessage());
    }
}