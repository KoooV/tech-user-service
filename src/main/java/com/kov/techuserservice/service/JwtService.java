package com.kov.techuserservice.service;

import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

public interface JwtService {

    <T> T extractClaim(String token, Function<Map<String, Object>, T> claimsResolver);

    Claims extractAllClaims(String token);

    String generateAccessToken(Long userId, String role, Instant now, Instant expiration);

    String generateRefreshToken(Long userId, Instant now, Instant expiration);

    boolean tokenIsValid(String token);
}