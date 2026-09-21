package com.kov.techuserservice.dto.role;

import com.kov.techuserservice.dto.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateDTO {

    @NotBlank
    private RoleName name;
}