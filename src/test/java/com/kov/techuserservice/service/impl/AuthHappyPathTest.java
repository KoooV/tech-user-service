package com.kov.techuserservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kov.techuserservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Happy-path сквозь реальные слои (контроллер → сервис → БД, настоящий JWT):
 * register → login → me → refresh → logout → refresh отозван.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthHappyPathTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerLoginMeRefreshLogout_ShouldWorkEndToEnd() throws Exception {
        String email = "happy-" + UUID.randomUUID() + "@example.com";

        // 1. register → 201 + пара токенов.
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Happy","lastName":"Path","email":"%s","phone":"+1234567890","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();
        JsonNode registeredBody = objectMapper.readTree(registered.getResponse().getContentAsString());
        String refreshToken = registeredBody.get("refreshToken").asText();

        // 2. login → 200 + новая пара токенов.
        MvcResult loggedIn = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        JsonNode loginBody = objectMapper.readTree(loggedIn.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();
        String loginRefresh = loginBody.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(loginRefresh).isNotBlank();

        // 3. me с access-токеном → 200, тот же email.
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // 4. refresh → 200, новый access-токен.
        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loginRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        String refreshedAccess = objectMapper.readTree(refreshed.getResponse().getContentAsString())
                .get("accessToken").asText();
        assertThat(refreshedAccess).isNotBlank();

        // 5. logout → 200.
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + refreshedAccess))
                .andExpect(status().isOk());

        // 6. refresh после logout → 401 (токен отозван).
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loginRefresh + "\"}"))
                .andExpect(status().isUnauthorized());

        // 7. Access-токен stateless: действует до истечения срока даже после logout.
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + refreshedAccess))
                .andExpect(status().isOk());

        // refresh-токен первой (register) сессии тоже отозван.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
