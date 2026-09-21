package com.kov.techuserservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderImplTest {

    private PasswordEncoderImpl passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordEncoderImpl();
    }

    @Test
    void encode_ShouldReturnEncodedPassword() {
        String rawPassword = "password123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
    }

    @Test
    void matches_ValidPassword_ShouldReturnTrue() {
        String rawPassword = "password123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    void matches_InvalidPassword_ShouldReturnFalse() {
        String rawPassword = "password123";
        String wrongPassword = "wrongPassword";
        String encoded = passwordEncoder.encode(rawPassword);

        assertFalse(passwordEncoder.matches(wrongPassword, encoded));
    }

    @Test
    void matches_NullPassword_ShouldReturnFalse() {
        assertFalse(passwordEncoder.matches(null, "encoded"));
        assertFalse(passwordEncoder.matches("raw", null));
    }

    @Test
    void encode_NullPassword_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> passwordEncoder.encode(null));
    }
}