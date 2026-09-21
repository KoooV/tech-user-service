package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.auth.AuthRefreshRequestDTO;
import com.kov.techuserservice.dto.auth.AuthRequestDTO;
import com.kov.techuserservice.dto.auth.AuthResponseDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.entity.Role;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.RefreshTokenRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import com.kov.techuserservice.exception.SecurityException;
import com.kov.techuserservice.exception.UserNotFoundException;
import com.kov.techuserservice.mapper.AuthMapper;
import com.kov.techuserservice.entity.RefreshToken;
import com.kov.techuserservice.security.JwtConfig;
import com.kov.techuserservice.security.PasswordEncoderImpl;
import com.kov.techuserservice.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.kov.techuserservice.service.RoleService roleService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PasswordEncoderImpl passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UserRequestDTO registerRequest;
    private AuthRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhone("+1234567890");
        testUser.setActive(true);

        Role role = new Role();
        role.setId(1L);
        role.setName(com.kov.techuserservice.dto.enums.RoleName.USER);
        testUser.setRoles(Collections.singleton(role));

        registerRequest = new UserRequestDTO();
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPhone("+1234567890");
        registerRequest.setPassword("password123");

        loginRequest = new AuthRequestDTO();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        when(jwtConfig.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtConfig.getRefreshTokenExpirationMs()).thenReturn(86400000L);
    }

    @Test
    void register_ShouldReturnAuthResponseDTO() {
        Role defaultRole = new Role();
        defaultRole.setId(1L);
        defaultRole.setName(com.kov.techuserservice.dto.enums.RoleName.USER);
        when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(false);
        when(roleService.getOrCreate(com.kov.techuserservice.dto.enums.RoleName.USER))
                .thenReturn(defaultRole);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(jwtService.generateRefreshToken(anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn("refresh-token");
        when(jwtService.generateAccessToken(anyLong(), anyString(), any(Instant.class), any(Instant.class)))
                .thenReturn("access-token");
        when(authMapper.toAuthResponse(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(new AuthResponseDTO("access-token", "refresh-token", "Bearer", 900000L));

        AuthResponseDTO response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void authenticate_ValidCredentials_ShouldReturnAuthResponseDTO() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtService.generateRefreshToken(anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn("refresh-token");
        when(jwtService.generateAccessToken(anyLong(), anyString(), any(Instant.class), any(Instant.class)))
                .thenReturn("access-token");
        when(refreshTokenRepository.findAllValidTokenByUserId(anyLong()))
                .thenReturn(Collections.emptyList());
        when(authMapper.toAuthResponse(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(new AuthResponseDTO("access-token", "refresh-token", "Bearer", 900000L));

        AuthResponseDTO response = authService.authenticate(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void authenticate_InvalidCredentials_ShouldThrowBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(loginRequest));
    }

    @Test
    void logout_ShouldRevokeAllRefreshTokens() {
        authService.logout(1L);
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(1L);
    }

    @Test
    void refreshToken_ValidToken_ShouldReturnNewAuthResponse() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .tokenVersion(0)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .revoked(false)
                .user(testUser)
                .build();

        when(jwtService.tokenIsValid("refresh-token")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtService.extractAllClaims("refresh-token")).thenReturn(claims);
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(anyLong(), anyString(), any(Instant.class), any(Instant.class)))
                .thenReturn("new-access-token");
        when(authMapper.toAuthResponse(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(new AuthResponseDTO("new-access-token", "refresh-token", "Bearer", 900000L));

        AuthRefreshRequestDTO request = new AuthRefreshRequestDTO();
        request.setRefreshToken("refresh-token");

        AuthResponseDTO response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
    }

    @Test
    void refreshToken_EmptyToken_ShouldThrowSecurityException() {
        AuthRefreshRequestDTO request = new AuthRefreshRequestDTO();
        request.setRefreshToken("");

        assertThrows(SecurityException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_InvalidToken_ShouldThrowSecurityException() {
        when(jwtService.tokenIsValid("invalid-token")).thenReturn(false);

        AuthRefreshRequestDTO request = new AuthRefreshRequestDTO();
        request.setRefreshToken("invalid-token");

        assertThrows(SecurityException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_RevokedToken_ShouldThrowSecurityException() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(1L)
                .token("revoked-token")
                .tokenVersion(0)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .revoked(true)
                .user(testUser)
                .build();

        when(jwtService.tokenIsValid("revoked-token")).thenReturn(true);
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(storedToken));

        AuthRefreshRequestDTO request = new AuthRefreshRequestDTO();
        request.setRefreshToken("revoked-token");

        assertThrows(SecurityException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_ExpiredToken_ShouldThrowSecurityException() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .tokenVersion(0)
                .expiresAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .revoked(false)
                .user(testUser)
                .build();

        when(jwtService.tokenIsValid("expired-token")).thenReturn(true);
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(storedToken));

        AuthRefreshRequestDTO request = new AuthRefreshRequestDTO();
        request.setRefreshToken("expired-token");

        assertThrows(SecurityException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_UserNotFound_ShouldThrowUserNotFoundException() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(1L)
                .token("valid-token")
                .tokenVersion(0)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .revoked(false)
                .user(testUser)
                .build();

        when(jwtService.tokenIsValid("valid-token")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtService.extractAllClaims("valid-token")).thenReturn(claims);
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AuthRefreshRequestDTO request = new AuthRefreshRequestDTO();
        request.setRefreshToken("valid-token");

        assertThrows(UserNotFoundException.class, () -> authService.refreshToken(request));
    }
}