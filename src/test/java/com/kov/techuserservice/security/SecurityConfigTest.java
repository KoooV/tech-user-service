package com.kov.techuserservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void securityConfig_ShouldBeCreated() throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                new com.kov.techuserservice.service.impl.JwtServiceImpl(new JwtConfig()),
                new CustomUserDetailsServiceImpl(null)
        );

        SecurityConfig config = new SecurityConfig(jwtFilter);
        assertNotNull(config);
    }
}