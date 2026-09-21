package com.kov.techuserservice.service;

import com.kov.techuserservice.dto.page.PageResponseDTO;
import com.kov.techuserservice.dto.role.RoleUpdateDTO;
import com.kov.techuserservice.dto.user.UpdateUserRequestDTO;
import com.kov.techuserservice.dto.user.UserRequestDTO;
import com.kov.techuserservice.dto.user.UserResponseDTO;
import org.springframework.data.domain.Pageable;

public interface UserService {

    PageResponseDTO<UserResponseDTO> getAllUsers(Pageable pageable);

    UserResponseDTO getUserById(Long id);

    UserResponseDTO updateUser(Long id, UserRequestDTO request);

    UserResponseDTO partialUpdateUser(Long id, UpdateUserRequestDTO request);

    void deleteUser(Long id);

    UserResponseDTO assignRole(Long id, RoleUpdateDTO request);

    UserResponseDTO toggleActive(Long id);

    void resetPassword(Long id);

    UserResponseDTO getCurrentUser();
}
