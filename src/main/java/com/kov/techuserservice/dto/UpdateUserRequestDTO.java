package com.kov.techuserservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDTO {

    private String firstName;

    private String lastName;

    @Email
    private String email;

    private String phone;

    @NotBlank
    private String currentPassword;

    @Size(min = 6)
    private String newPassword;
}
