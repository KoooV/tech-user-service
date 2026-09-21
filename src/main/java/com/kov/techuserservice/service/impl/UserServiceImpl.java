package com.kov.techuserservice.service.impl;

import com.kov.techuserservice.dto.page.PageResponseDTO;
import com.kov.techuserservice.dto.role.RoleUpdateDTO;
import com.kov.techuserservice.dto.user.UpdateUserRequestDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.RefreshTokenRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import com.kov.techuserservice.exception.SecurityException;
import com.kov.techuserservice.exception.UserNotFoundException;
import com.kov.techuserservice.mapper.UserMapper;
import com.kov.techuserservice.security.PasswordEncoderImpl;
import com.kov.techuserservice.service.RoleService;
import com.kov.techuserservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoderImpl passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        PageResponseDTO<UserResponseDTO> response = new PageResponseDTO<>();
        response.setContent(users.map(userMapper::toResponse).getContent());
        response.setPage(users.getNumber());
        response.setSize(users.getSize());
        response.setTotalElements(users.getTotalElements());
        response.setTotalPages(users.getTotalPages());
        response.setLast(users.isLast());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO request) {
        User user = findUserOrThrow(id);
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new SecurityException("Email already in use: " + request.getEmail());
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        User saved = userRepository.save(user);
        log.info("User {} fully updated", id);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponseDTO partialUpdateUser(Long id, UpdateUserRequestDTO request) {
        User user = findUserOrThrow(id);
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new SecurityException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getCurrentPassword() != null && request.getNewPassword() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new SecurityException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        User saved = userRepository.save(user);
        log.info("User {} partially updated", id);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("User {} deleted", id);
    }

    @Override
    @Transactional
    public UserResponseDTO assignRole(Long id, RoleUpdateDTO request) {
        User user = findUserOrThrow(id);
        var role = roleService.findByName(request.getName())
                .orElseThrow(() -> new UserNotFoundException("Role not found: " + request.getName()));
        // Merge-семантика: роль добавляется к уже назначенным, существующие не затираются.
        // Set.of(role) здесь был багом: immutable + потеря всех предыдущих ролей.
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(role);
        User saved = userRepository.save(user);
        log.info("Role {} assigned to user {} (roles now: {})", request.getName(), id, user.getRoles().size());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponseDTO removeRole(Long id, RoleUpdateDTO request) {
        User user = findUserOrThrow(id);
        var role = roleService.findByName(request.getName())
                .orElseThrow(() -> new UserNotFoundException("Role not found: " + request.getName()));
        if (user.getRoles() == null || !user.getRoles().contains(role)) {
            throw new UserNotFoundException("User " + id + " does not have role: " + request.getName());
        }
        if (user.getRoles().size() <= 1) {
            throw new SecurityException("Cannot remove the last role of user " + id);
        }
        user.getRoles().remove(role);
        User saved = userRepository.save(user);
        log.info("Role {} removed from user {} (roles now: {})", request.getName(), id, user.getRoles().size());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponseDTO toggleActive(Long id) {
        User user = findUserOrThrow(id);
        user.setActive(!user.isActive());
        User saved = userRepository.save(user);
        log.info("User {} active flag toggled to {}", id, saved.isActive());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        User user = findUserOrThrow(id);
        // Инвалидируем все refresh-токены, чтобы завершить активные сессии.
        // Генерация/отправка нового пароля — зона интеграций (notification service),
        // здесь фиксируем сам факт сброса.
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password reset requested for user {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser() {
        User user = resolveCurrentUser();
        return userMapper.toResponse(user);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + userDetails.getUsername()));
        }
        try {
            Long userId = Long.valueOf(authentication.getName());
            return findUserOrThrow(userId);
        } catch (NumberFormatException e) {
            throw new SecurityException("Unable to resolve current user");
        }
    }
}
