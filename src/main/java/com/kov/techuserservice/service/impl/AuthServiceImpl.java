package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.auth.AuthRefreshRequestDTO;
import com.kov.techuserservice.dto.auth.AuthRequestDTO;
import com.kov.techuserservice.dto.auth.AuthResponseDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.RefreshTokenRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import com.kov.techuserservice.exception.SecurityException;
import com.kov.techuserservice.exception.UserNotFoundException;
import com.kov.techuserservice.mapper.AuthMapper;
import com.kov.techuserservice.model.RefreshToken;
import com.kov.techuserservice.security.PasswordEncoderImpl;
import com.kov.techuserservice.service.AuthService;
import com.kov.techuserservice.service.JwtService;
import com.kov.techuserservice.security.JwtConfig;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoderImpl passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final AuthenticationManager authenticationManager;

    private static final String DEFAULT_ROLE = "USER";

    private String extractRoleName(User user) {
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            return user.getRoles().iterator().next().getName().name();
        }
        return DEFAULT_ROLE;
    }

    @Override
    @Transactional
    public AuthResponseDTO register(UserRequestDTO request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setRoles(Collections.emptySet());
        User saved = userRepository.save(user);

        Instant now = Instant.now();
        Instant refreshExpiration = now.plusMillis(jwtConfig.getRefreshTokenExpirationMs());
        Instant accessExpiration = now.plusMillis(jwtConfig.getAccessTokenExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(saved)
                .tokenVersion(0)
                .token(jwtService.generateRefreshToken(saved.getId(), now, refreshExpiration))
                .expiresAt(refreshExpiration)
                .createdAt(now)
                .build();
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateAccessToken(saved.getId(), extractRoleName(saved), now, accessExpiration);
        log.info("User registered with id: {}", saved.getId());
        return authMapper.toAuthResponse(accessToken, refreshToken.getToken(), "Bearer", accessExpiration.toEpochMilli());
    }

    @Override
    @Transactional
    public AuthResponseDTO authenticate(AuthRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = (User) authentication.getPrincipal();

            Instant now = Instant.now();
            Instant refreshExpiration = now.plusMillis(jwtConfig.getRefreshTokenExpirationMs());
            Instant accessExpiration = now.plusMillis(jwtConfig.getAccessTokenExpirationMs());

            long nextTokenVersion = refreshTokenRepository.findAllValidTokenByUserId(user.getId())
                    .stream()
                    .mapToLong(RefreshToken::getTokenVersion)
                    .max()
                    .orElse(0L) + 1L;

            String refreshTokenValue = jwtService.generateRefreshToken(user.getId(), now, refreshExpiration);
            RefreshToken refreshToken = RefreshToken.builder()
                    .user(user)
                    .tokenVersion(nextTokenVersion)
                    .token(refreshTokenValue)
                    .expiresAt(refreshExpiration)
                    .createdAt(now)
                    .build();
            refreshTokenRepository.save(refreshToken);

            String accessToken = jwtService.generateAccessToken(user.getId(), extractRoleName(user), now, accessExpiration);

            return authMapper.toAuthResponse(accessToken, refreshTokenValue, "Bearer", accessExpiration.toEpochMilli());
        } catch (AuthenticationException e) {
            log.warn("Failed to authenticate user with email {}: {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("User {} logged out: all refresh tokens revoked", userId);
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(AuthRefreshRequestDTO request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new SecurityException("Refresh token is empty");
        }

        if (!jwtService.tokenIsValid(request.getRefreshToken())) {
            throw new SecurityException("Invalid refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new SecurityException("Refresh token not found"));

        if (stored.isRevoked()) {
            throw new SecurityException("Refresh token revoked");
        }
        if (stored.isExpired()) {
            refreshTokenRepository.revokeByToken(request.getRefreshToken());
            throw new SecurityException("Refresh token expired");
        }

        Claims claims = jwtService.extractAllClaims(request.getRefreshToken());
        Long userId;
        try {
            userId = claims.getSubject() != null ? Long.valueOf(claims.getSubject()) : null;
        } catch (Exception e) {
            throw new SecurityException("Invalid refresh token subject (userId)");
        }

        if (userId == null) {
            throw new SecurityException("Invalid refresh token subject");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found for refresh token"));

        if (stored.getUser() != null && stored.getUser().getId() != null && !stored.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Refresh token does not belong to this user");
        }

        Instant now = Instant.now();
        Instant accessExpiration = now.plusMillis(jwtConfig.getAccessTokenExpirationMs());

        String roleName = extractRoleName(user);
        String newAccessToken = jwtService.generateAccessToken(user.getId(), roleName, now, accessExpiration);

        return authMapper.toAuthResponse(newAccessToken, stored.getToken(), "Bearer", accessExpiration.toEpochMilli());
    }

    @Override
    public java.util.UUID getCurrentUserId() {
        throw new UnsupportedOperationException("Метод getCurrentUserId еще не реализован");
    }
}