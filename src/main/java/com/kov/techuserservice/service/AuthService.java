package com.kov.techuserservice.service;

import com.kov.techuserservice.dto.auth.AuthRefreshRequestDTO;
import com.kov.techuserservice.dto.auth.AuthRequestDTO;
import com.kov.techuserservice.dto.auth.AuthResponseDTO;

public interface AuthService {

    AuthResponseDTO register(com.kov.techuserservice.dto.user.UserRequestDTO request);

    AuthResponseDTO authenticate(AuthRequestDTO request);

    void logout(Long userId);

    AuthResponseDTO refreshToken(AuthRefreshRequestDTO request);

    Long getCurrentUserId();
}