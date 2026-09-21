package com.kov.techuserservice.controller;

import com.kov.techuserservice.dto.page.PageResponseDTO;
import com.kov.techuserservice.dto.role.RoleDTO;
import com.kov.techuserservice.dto.role.RoleUpdateDTO;
import com.kov.techuserservice.dto.user.*;
import com.kov.techuserservice.entity.Role;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.UserRepository;
import com.kov.techuserservice.mapper.RoleMapper;
import com.kov.techuserservice.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    @GetMapping
    public ResponseEntity<PageResponseDTO<UserResponseDTO>> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        PageResponseDTO<UserResponseDTO> response = new PageResponseDTO<>();
        response.setContent(users.map(userMapper::toResponse).getContent());
        response.setPage(users.getPageable().getPageNumber());
        response.setSize(users.getPageable().getPageSize());
        response.setTotalElements(users.getTotalElements());
        response.setTotalPages(users.getTotalPages());
        response.setLast(users.isLast());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(userMapper.toResponse(user)))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDTO request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setEmail(request.getEmail());
                    user.setPhone(request.getPhone());
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(userMapper.toResponse(saved));
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponseDTO> partialUpdateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequestDTO request) {
        return userRepository.findById(id)
                .map(user -> {
                    if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
                    if (request.getLastName() != null) user.setLastName(request.getLastName());
                    if (request.getEmail() != null) user.setEmail(request.getEmail());
                    if (request.getPhone() != null) user.setPhone(request.getPhone());
                    if (request.getCurrentPassword() != null && request.getNewPassword() != null) {
                        user.setPassword(request.getNewPassword());
                    }
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(userMapper.toResponse(saved));
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponseDTO> assignRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO request) {
        return userRepository.findById(id)
                .map(user -> {
                    Role role = roleMapper.toEntity(request);
                    user.setRoles(Set.of(role));
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(userMapper.toResponse(saved));
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<UserResponseDTO> toggleActive(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(!user.isActive());
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(userMapper.toResponse(saved));
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Void>(HttpStatus.OK);
    }
}