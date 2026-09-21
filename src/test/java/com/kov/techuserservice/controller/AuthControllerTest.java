package com.kov.techuserservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kov.techuserservice.TestcontainersConfiguration;
import com.kov.techuserservice.dto.auth.AuthRefreshRequestDTO;
import com.kov.techuserservice.dto.auth.AuthRequestDTO;
import com.kov.techuserservice.dto.auth.AuthResponseDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.exception.DuplicateEmailException;
import com.kov.techuserservice.service.AuthService;
import com.kov.techuserservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Контроллерные тесты в полном Spring-контексте (а не {@code standaloneSetup}):
 * проверяется реальный Jackson, Bean Validation и {@code GlobalExceptionHandler}
 * (контракт {@code ApiError}). Сервисный слой замокирован.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @Test
    void register_ShouldReturnCreatedResponse() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .firstName("Test").lastName("User")
                .email("test@example.com").phone("+1234567890")
                .password("password123")
                .build();

        AuthResponseDTO authResponse = new AuthResponseDTO("access-token", "refresh-token", "Bearer", 900000L);
        when(authService.register(any(UserRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(authService, times(1)).register(any(UserRequestDTO.class));
    }

    @Test
    void register_DuplicateEmail_ShouldReturn409ApiError() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .firstName("Test").lastName("User")
                .email("taken@example.com").phone("+1234567890")
                .password("password123")
                .build();
        when(authService.register(any(UserRequestDTO.class)))
                .thenThrow(new DuplicateEmailException("Email already in use: taken@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Email already in use: taken@example.com"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void register_InvalidBody_ShouldReturn400WithAllErrors() throws Exception {
        // Пустой объект: срабатывают сразу несколько @NotBlank — контракт требует вернуть ВСЕ.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(authService, never()).register(any());
    }

    @Test
    void login_ShouldReturnOkResponse() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponseDTO authResponse = new AuthResponseDTO("access-token", "refresh-token", "Bearer", 900000L);
        when(authService.authenticate(any(AuthRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
