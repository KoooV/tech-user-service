package com.kov.techuserservice.mapper;

import com.kov.techuserservice.dto.auth.AuthResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    AuthMapper INSTANCE = Mappers.getMapper(AuthMapper.class);

    AuthResponseDTO toAuthResponse(String accessToken, String refreshToken, String tokenType, Long expiresIn);
}