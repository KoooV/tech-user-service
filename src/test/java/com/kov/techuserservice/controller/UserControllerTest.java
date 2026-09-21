package com.kov.techuserservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kov.techuserservice.TestcontainersConfiguration;
import com.kov.techuserservice.dto.enums.RoleName;
import com.kov.techuserservice.dto.page.PageResponseDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;
import com.kov.techuserservice.entity.Role;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.exception.DuplicateEmailException;
import com.kov.techuserservice.exception.UserNotFoundException;
import com.kov.techuserservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@code UserController} в полном Spring-контексте: реальный Security-фильтр,
 * {@code @PreAuthorize} и {@code GlobalExceptionHandler}. Сервис замокирован.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    private static User userPrincipal(Long id, RoleName roleName) {
        Role role = new Role();
        role.setId(1L);
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setRoles(Set.of(role));
        return user;
    }

    private static UsernamePasswordAuthenticationToken auth(Long id, RoleName roleName) {
        User principal = userPrincipal(id, roleName);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void getUserById_Unauthenticated_ShouldReturn401ApiError() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/users/1"));
    }

    @Test
    void getAllUsers_UserRole_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users").with(authentication(auth(1L, RoleName.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void getAllUsers_AdminRole_ShouldReturn200() throws Exception {
        PageResponseDTO<UserResponseDTO> page = PageResponseDTO.<UserResponseDTO>builder()
                .content(List.of(UserResponseDTO.builder().id(1L).email("user1@example.com").build()))
                .page(0).size(10).totalElements(1).totalPages(1).last(true)
                .build();
        when(userService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/users").with(authentication(auth(1L, RoleName.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user1@example.com"));
    }

    @Test
    void getUserById_Self_ShouldReturn200() throws Exception {
        when(userService.getUserById(1L))
                .thenReturn(UserResponseDTO.builder().id(1L).email("user1@example.com").build());

        mockMvc.perform(get("/api/users/1").with(authentication(auth(1L, RoleName.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user1@example.com"));
    }

    @Test
    void getUserById_OtherUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users/2").with(authentication(auth(1L, RoleName.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getUserById_Missing_ShouldReturn404ApiError() throws Exception {
        when(userService.getUserById(42L)).thenThrow(new UserNotFoundException("User not found with id: 42"));

        mockMvc.perform(get("/api/users/42").with(authentication(auth(42L, RoleName.ADMIN))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: 42"));
    }

    @Test
    void deleteUser_UserRole_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/users/1").with(authentication(auth(1L, RoleName.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void updateUser_InvalidBody_ShouldReturn400WithAllErrors() throws Exception {
        mockMvc.perform(put("/api/users/1").with(authentication(auth(1L, RoleName.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void updateUser_DuplicateEmail_ShouldReturn409() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .firstName("New").lastName("Name")
                .email("taken@example.com").phone("+1999999999")
                .password("password123")
                .build();
        when(userService.updateUser(eq(1L), any(UserRequestDTO.class)))
                .thenThrow(new DuplicateEmailException("Email already in use: taken@example.com"));

        mockMvc.perform(put("/api/users/1").with(authentication(auth(1L, RoleName.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
