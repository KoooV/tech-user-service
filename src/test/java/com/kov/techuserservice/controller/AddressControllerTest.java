package com.kov.techuserservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kov.techuserservice.TestcontainersConfiguration;
import com.kov.techuserservice.dto.address.AddressDTO;
import com.kov.techuserservice.dto.enums.RoleName;
import com.kov.techuserservice.entity.Role;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.exception.AddressNotFoundException;
import com.kov.techuserservice.service.AddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@code AddressController} в полном Spring-контексте: реальный Security-фильтр,
 * проверка владельца через {@code @PreAuthorize} и {@code GlobalExceptionHandler}.
 * Сервис замокирован.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AddressService addressService;

    private static UsernamePasswordAuthenticationToken auth(Long id, RoleName roleName) {
        Role role = new Role();
        role.setId(1L);
        role.setName(roleName);
        User principal = new User();
        principal.setId(id);
        principal.setEmail("user" + id + "@example.com");
        principal.setRoles(Set.of(role));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void getAllAddresses_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/1/addresses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void getAllAddresses_Self_ShouldReturn200() throws Exception {
        when(addressService.getAllAddresses(1L)).thenReturn(List.of(
                AddressDTO.builder().id(7L).country("KZ").city("Almaty").street("Abaya 1").build()));

        mockMvc.perform(get("/api/users/1/addresses").with(authentication(auth(1L, RoleName.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Almaty"));
    }

    @Test
    void getAllAddresses_OtherUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users/2/addresses").with(authentication(auth(1L, RoleName.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAllAddresses_ManagerReadingOthers_ShouldReturn200() throws Exception {
        when(addressService.getAllAddresses(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/2/addresses").with(authentication(auth(1L, RoleName.MANAGER))))
                .andExpect(status().isOk());
    }

    @Test
    void addAddress_Valid_ShouldReturn201() throws Exception {
        AddressDTO request = AddressDTO.builder()
                .country("KZ").city("Astana").street("Mangilik 1").label("Home")
                .build();
        when(addressService.addAddress(eq(1L), any(AddressDTO.class)))
                .thenReturn(AddressDTO.builder().id(9L)
                        .country("KZ").city("Astana").street("Mangilik 1").label("Home").build());

        mockMvc.perform(post("/api/users/1/addresses").with(authentication(auth(1L, RoleName.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9L));
    }

    @Test
    void addAddress_InvalidBody_ShouldReturn400WithAllErrors() throws Exception {
        // Флаг isDefault передан (иначе упадёт десериализация primitive boolean),
        // отсутствуют сразу несколько @NotBlank — контракт требует вернуть ВСЕ.
        mockMvc.perform(post("/api/users/1/addresses").with(authentication(auth(1L, RoleName.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isDefault\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.country").exists())
                .andExpect(jsonPath("$.errors.city").exists())
                .andExpect(jsonPath("$.errors.street").exists());
    }

    @Test
    void getDefaultAddress_Missing_ShouldReturn404() throws Exception {
        when(addressService.getDefaultAddress(1L))
                .thenThrow(new AddressNotFoundException("Default address not found for user: 1"));

        mockMvc.perform(get("/api/users/1/addresses/default").with(authentication(auth(1L, RoleName.USER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
