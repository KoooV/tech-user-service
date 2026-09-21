package com.kov.techuserservice.security;

import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsServiceImpl userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhone("+1234567890");
        testUser.setActive(true);
        testUser.setPassword("$2a$10$hashedPassword");
    }

    @Test
    void loadUserByUsername_ExistingUser_ShouldReturnUserDetails() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_NonExistingUser_ShouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(java.util.Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("unknown@example.com"));
    }

    @Test
    void loadUserById_ExistingUser_ShouldReturnUserDetails() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserById(1L);

        assertNotNull(userDetails);
        assertEquals(1L, ((User) userDetails).getId());
    }

    @Test
    void loadUserById_NonExistingUser_ShouldThrowUsernameNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserById(999L));
    }

    @Test
    void loadUserByUsername_InactiveUser_ShouldNotBeEnabled() {
        testUser.setActive(false);
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(java.util.Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("inactive@example.com");

        assertFalse(userDetails.isEnabled());
    }
}