package com.kov.techuserservice.controller;

import com.kov.techuserservice.dto.auth.AuthRefreshRequestDTO;
import com.kov.techuserservice.dto.auth.AuthRequestDTO;
import com.kov.techuserservice.dto.auth.AuthResponseDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;
import com.kov.techuserservice.mapper.UserMapper;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO request) {
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setRoles(Collections.emptySet());
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = (User) authentication.getPrincipal();
        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken("access-token");
        response.setRefreshToken("refresh-token");
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(@Valid @RequestBody AuthRefreshRequestDTO request) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken("new-access-token");
        response.setRefreshToken(request.getRefreshToken());
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
}