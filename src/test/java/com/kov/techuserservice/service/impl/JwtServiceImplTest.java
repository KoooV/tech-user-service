package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.security.JwtConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtServiceImplTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn("5A7C3F9D2E6B8A1C4F7D0E3B9A2C5F8D1E4B7A0C3F6D9B2E5A8C1F4D7B0E3A6");
        when(jwtConfig.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtConfig.getRefreshTokenExpirationMs()).thenReturn(86400000L);
    }

    @Test
    void extractAllClaims_ValidToken_ShouldReturnClaims() {
        String token = jwtService.generateAccessToken(1L, "USER", Instant.now(), Instant.now().plusSeconds(3600));
        Claims claims = jwtService.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
    }

    @Test
    void extractClaim_ValidToken_ShouldReturnClaim() {
        String token = jwtService.generateAccessToken(1L, "USER", Instant.now(), Instant.now().plusSeconds(3600));
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("USER", role);
    }

    @Test
    void generateAccessToken_ShouldReturnValidToken() {
        String token = jwtService.generateAccessToken(1L, "USER", Instant.now(), Instant.now().plusSeconds(3600));
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        String token = jwtService.generateRefreshToken(1L, Instant.now(), Instant.now().plusSeconds(86400));
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void tokenIsValid_ValidToken_ShouldReturnTrue() {
        String token = jwtService.generateAccessToken(1L, "USER", Instant.now(), Instant.now().plusSeconds(3600));
        assertTrue(jwtService.tokenIsValid(token));
    }

    @Test
    void tokenIsValid_InvalidToken_ShouldReturnFalse() {
        assertFalse(jwtService.tokenIsValid("invalid.token.value"));
    }

    @Test
    void tokenIsValid_ExpiredToken_ShouldReturnFalse() {
        String token = jwtService.generateAccessToken(
                1L, "USER", Instant.now(), Instant.now().minusSeconds(1)
        );
        assertFalse(jwtService.tokenIsValid(token));
    }

    @Test
    void extractAllClaims_InvalidToken_ShouldThrowException() {
        assertThrows(Exception.class, () -> jwtService.extractAllClaims("invalid.token"));
    }

    @Test
    void extractClaim_InvalidToken_ShouldThrowException() {
        assertThrows(Exception.class, () ->
                jwtService.extractClaim("invalid.token", claims -> claims.getSubject()));
    }
}