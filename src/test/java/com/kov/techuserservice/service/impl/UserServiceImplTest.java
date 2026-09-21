package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.enums.RoleName;
import com.kov.techuserservice.dto.page.PageResponseDTO;
import com.kov.techuserservice.dto.role.RoleUpdateDTO;
import com.kov.techuserservice.dto.user.UpdateUserRequestDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;
import com.kov.techuserservice.entity.Role;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.RefreshTokenRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import com.kov.techuserservice.exception.DuplicateEmailException;
import com.kov.techuserservice.exception.SecurityException;
import com.kov.techuserservice.exception.UserNotFoundException;
import com.kov.techuserservice.mapper.UserMapper;
import com.kov.techuserservice.security.PasswordEncoderImpl;
import com.kov.techuserservice.service.NotificationService;
import com.kov.techuserservice.service.RoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoderImpl passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setPhone("+1234567890");
        testUser.setPassword("$2a$10$encodedOld");
        testUser.setActive(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_ShouldReturnMappedPage() {
        User second = new User();
        second.setId(2L);
        second.setEmail("second@example.com");

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(testUser, second), pageable, 2);
        when(userRepository.findAll(pageable)).thenReturn(page);

        UserResponseDTO dto1 = UserResponseDTO.builder().id(1L).email("test@example.com").build();
        UserResponseDTO dto2 = UserResponseDTO.builder().id(2L).email("second@example.com").build();
        when(userMapper.toResponse(testUser)).thenReturn(dto1);
        when(userMapper.toResponse(second)).thenReturn(dto2);

        PageResponseDTO<UserResponseDTO> result = userService.getAllUsers(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLast());
        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    void getUserById_ExistingUser_ShouldReturnDto() {
        UserResponseDTO dto = UserResponseDTO.builder().id(1L).email("test@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(dto);

        assertEquals(dto, userService.getUserById(1L));
    }

    @Test
    void getUserById_MissingUser_ShouldThrow() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(42L));
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUser_SameEmail_ShouldUpdateWithoutDuplicateCheck() {
        UserRequestDTO request = UserRequestDTO.builder()
                .firstName("New").lastName("Name")
                .email("test@example.com").phone("+1999999999")
                .password("ignored")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        UserResponseDTO dto = UserResponseDTO.builder().id(1L).firstName("New").build();
        when(userMapper.toResponse(testUser)).thenReturn(dto);

        UserResponseDTO result = userService.updateUser(1L, request);

        assertEquals("New", result.getFirstName());
        assertEquals("+1999999999", testUser.getPhone());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateUser_DuplicateEmail_ShouldThrow() {
        UserRequestDTO request = UserRequestDTO.builder()
                .firstName("New").lastName("Name")
                .email("taken@example.com").phone("+1999999999")
                .password("ignored")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void partialUpdate_OnlyProvidedFields_ShouldMerge() {
        UpdateUserRequestDTO request = UpdateUserRequestDTO.builder()
                .firstName("Partial")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponseDTO.builder().id(1L).build());

        userService.partialUpdateUser(1L, request);

        assertEquals("Partial", testUser.getFirstName());
        assertEquals("User", testUser.getLastName());
        assertEquals("test@example.com", testUser.getEmail());
        assertEquals("$2a$10$encodedOld", testUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void partialUpdate_CorrectCurrentPassword_ShouldEncodeNewPassword() {
        UpdateUserRequestDTO request = UpdateUserRequestDTO.builder()
                .currentPassword("old-pass")
                .newPassword("new-pass-123")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("old-pass", "$2a$10$encodedOld")).thenReturn(true);
        when(passwordEncoder.encode("new-pass-123")).thenReturn("$2a$10$encodedNew");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponseDTO.builder().id(1L).build());

        userService.partialUpdateUser(1L, request);

        assertEquals("$2a$10$encodedNew", testUser.getPassword());
    }

    @Test
    void partialUpdate_WrongCurrentPassword_ShouldThrow() {
        UpdateUserRequestDTO request = UpdateUserRequestDTO.builder()
                .currentPassword("wrong")
                .newPassword("new-pass-123")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "$2a$10$encodedOld")).thenReturn(false);

        assertThrows(SecurityException.class, () -> userService.partialUpdateUser(1L, request));
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void partialUpdate_DuplicateEmail_ShouldThrow() {
        UpdateUserRequestDTO request = UpdateUserRequestDTO.builder()
                .email("taken@example.com")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.partialUpdateUser(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_ExistingUser_ShouldDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(refreshTokenRepository, times(1)).deleteByUser_Id(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_MissingUser_ShouldThrow() {
        when(userRepository.existsById(42L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(42L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void assignRole_ExistingRole_ShouldAddRole() {
        Role admin = new Role();
        admin.setId(7L);
        admin.setName(RoleName.ADMIN);
        RoleUpdateDTO request = RoleUpdateDTO.builder().name(RoleName.ADMIN).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.findByName(RoleName.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponseDTO.builder().id(1L).build());

        userService.assignRole(1L, request);

        assertEquals(Collections.singleton(admin), testUser.getRoles());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void assignRole_ShouldPreserveExistingRoles() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleName.USER);
        testUser.setRoles(new java.util.HashSet<>(Collections.singleton(userRole)));

        Role admin = new Role();
        admin.setId(7L);
        admin.setName(RoleName.ADMIN);
        RoleUpdateDTO request = RoleUpdateDTO.builder().name(RoleName.ADMIN).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.findByName(RoleName.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponseDTO.builder().id(1L).build());

        userService.assignRole(1L, request);

        assertTrue(testUser.getRoles().contains(userRole));
        assertTrue(testUser.getRoles().contains(admin));
        assertEquals(2, testUser.getRoles().size());
    }

    @Test
    void assignRole_MissingRole_ShouldThrow() {
        RoleUpdateDTO request = RoleUpdateDTO.builder().name(RoleName.MANAGER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.findByName(RoleName.MANAGER)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.assignRole(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void removeRole_ExistingRole_ShouldRemoveOnlyThatRole() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleName.USER);
        Role admin = new Role();
        admin.setId(7L);
        admin.setName(RoleName.ADMIN);
        testUser.setRoles(new java.util.HashSet<>(java.util.Set.of(userRole, admin)));
        RoleUpdateDTO request = RoleUpdateDTO.builder().name(RoleName.ADMIN).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.findByName(RoleName.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponseDTO.builder().id(1L).build());

        userService.removeRole(1L, request);

        assertTrue(testUser.getRoles().contains(userRole));
        assertFalse(testUser.getRoles().contains(admin));
        assertEquals(1, testUser.getRoles().size());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void removeRole_LastRole_ShouldThrow() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleName.USER);
        testUser.setRoles(new java.util.HashSet<>(Collections.singleton(userRole)));
        RoleUpdateDTO request = RoleUpdateDTO.builder().name(RoleName.USER).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));

        assertThrows(SecurityException.class, () -> userService.removeRole(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void removeRole_RoleNotAssigned_ShouldThrow() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleName.USER);
        testUser.setRoles(new java.util.HashSet<>(Collections.singleton(userRole)));
        Role admin = new Role();
        admin.setId(7L);
        admin.setName(RoleName.ADMIN);
        RoleUpdateDTO request = RoleUpdateDTO.builder().name(RoleName.ADMIN).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.findByName(RoleName.ADMIN)).thenReturn(Optional.of(admin));

        assertThrows(UserNotFoundException.class, () -> userService.removeRole(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void toggleActive_ShouldFlipFlag() {
        testUser.setActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponseDTO.builder().id(1L).active(false).build());

        UserResponseDTO result = userService.toggleActive(1L);

        assertFalse(testUser.isActive());
        assertFalse(result.isActive());
    }

    @Test
    void resetPassword_ExistingUser_ShouldRevokeTokens() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$encodedTemp");
        when(userRepository.save(testUser)).thenReturn(testUser);

        userService.resetPassword(1L);

        assertEquals("$2a$10$encodedTemp", testUser.getPassword());
        verify(userRepository, times(1)).save(testUser);
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(1L);
        verify(notificationService, times(1)).sendPasswordReset(any());
    }

    @Test
    void resetPassword_MissingUser_ShouldThrowWithoutRevoking() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.resetPassword(42L));
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
        verify(notificationService, never()).sendPasswordReset(any());
    }

    @Test
    void getCurrentUser_PrincipalIsUserEntity_ShouldReturnDtoWithoutQuery() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of()));
        UserResponseDTO dto = UserResponseDTO.builder().id(1L).email("test@example.com").build();
        when(userMapper.toResponse(testUser)).thenReturn(dto);

        assertEquals(dto, userService.getCurrentUser());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void getCurrentUser_PrincipalIsUserDetails_ShouldResolveByEmail() {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        "test@example.com", "secret", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        UserResponseDTO dto = UserResponseDTO.builder().id(1L).build();
        when(userMapper.toResponse(testUser)).thenReturn(dto);

        assertEquals(dto, userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_Unauthenticated_ShouldThrow() {
        SecurityContextHolder.clearContext();

        assertThrows(SecurityException.class, () -> userService.getCurrentUser());
    }
}
