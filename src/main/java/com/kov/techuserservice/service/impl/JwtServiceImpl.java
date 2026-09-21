package com.kov.techuserservice.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.kov.techuserservice.security.JwtConfig;
import com.kov.techuserservice.service.JwtService;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtConfig jwtConfig;
    private SecretKey secretKey;

    @Override
    public synchronized <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        ensureSecretKey();
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public synchronized Claims extractAllClaims(String token) {
        ensureSecretKey();
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public synchronized String generateAccessToken(Long userId, String role, Instant now, Instant expiration) {
        ensureSecretKey();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public synchronized String generateRefreshToken(Long userId, Instant now, Instant expiration) {
        ensureSecretKey();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public synchronized boolean tokenIsValid(String token) {
        ensureSecretKey();
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid jwt token: {}", e.getMessage());
            return false;
        }
    }

    private void ensureSecretKey() {
        if (secretKey == null) {
            byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }
}