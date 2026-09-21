package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.UserRepository;
import com.kov.techuserservice.exception.SecurityException;
import com.kov.techuserservice.exception.UserNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceCurrentUserTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_PrincipalIsUserEntity_ShouldReturnId() {
        User user = new User();
        user.setId(5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertEquals(5L, authService.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_PrincipalIsUserDetails_ShouldResolveByEmail() {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        "test@example.com", "secret", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        User user = new User();
        user.setId(3L);
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertEquals(3L, authService.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_UnknownEmail_ShouldThrowUserNotFound() {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        "ghost@example.com", "secret", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_NoAuthentication_ShouldThrowSecurity() {
        SecurityContextHolder.clearContext();

        assertThrows(SecurityException.class, () -> authService.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_UnparseableName_ShouldThrowSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null, List.of()));

        assertThrows(SecurityException.class, () -> authService.getCurrentUserId());
    }
}
