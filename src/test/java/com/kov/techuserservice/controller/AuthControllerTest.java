package com.kov.techuserservice.controller;

import com.kov.techuserservice.dto.auth.AuthRefreshRequestDTO;
import com.kov.techuserservice.dto.auth.AuthRequestDTO;
import com.kov.techuserservice.dto.auth.AuthResponseDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;
import com.kov.techuserservice.service.AuthService;
import com.kov.techuserservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void register_ShouldReturnCreatedResponse() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setEmail("test@example.com");
        request.setPhone("+1234567890");
        request.setPassword("password123");

        AuthResponseDTO authResponse = new AuthResponseDTO("access-token", "refresh-token", "Bearer", 900000L);
        when(authService.register(any(UserRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"test@example.com\",\"phone\":\"+1234567890\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(authService, times(1)).register(any(UserRequestDTO.class));
    }

    @Test
    void login_ShouldReturnOkResponse() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponseDTO authResponse = new AuthResponseDTO("access-token", "refresh-token", "Bearer", 900000L);
        when(authService.authenticate(any(AuthRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(authService, times(1)).authenticate(any(AuthRequestDTO.class));
    }

    @Test
    void refreshToken_ShouldReturnOkResponse() throws Exception {
        AuthRefreshRequestDTO request = new AuthRefreshRequestDTO();
        request.setRefreshToken("refresh-token");

        AuthResponseDTO authResponse = new AuthResponseDTO("new-access-token", "refresh-token", "Bearer", 900000L);
        when(authService.refreshToken(any(AuthRefreshRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));

        verify(authService, times(1)).refreshToken(any(AuthRefreshRequestDTO.class));
    }

    @Test
    void logout_ShouldReturnOkResponse() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(1L);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());

        verify(authService, times(1)).logout(1L);
    }
}