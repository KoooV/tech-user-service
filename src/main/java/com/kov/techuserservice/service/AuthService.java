package com.kov.techuserservice.service;

import com.kov.techuserservice.dto.auth.AuthRefreshRequestDTO;
import com.kov.techuserservice.dto.auth.AuthRequestDTO;
import com.kov.techuserservice.dto.auth.AuthResponseDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;

import java.util.UUID;

public interface AuthService {

    AuthResponseDTO register(com.kov.techuserservice.dto.user.UserRequestDTO request);

    AuthResponseDTO authenticate(AuthRequestDTO request);

    void logout(Long userId);

    AuthResponseDTO refreshToken(AuthRefreshRequestDTO request);

    UUID getCurrentUserId();
}