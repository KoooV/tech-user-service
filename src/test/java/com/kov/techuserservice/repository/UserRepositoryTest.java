package com.kov.techuserservice.repository;

import com.kov.techuserservice.TestcontainersConfiguration;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Репозиторные тесты на реальной БД (Testcontainers + PostgreSQL):
 * CRUD, уникальность email, производные запросы.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private static User newUser(String email) {
        User user = new User();
        user.setFirstName("Repo");
        user.setLastName("Test");
        user.setEmail(email);
        user.setPassword("$2a$10$hashed");
        user.setPhone("+1234567890");
        user.setActive(true);
        return user;
    }

    @Test
    void saveAndFindByEmail_ShouldRoundTrip() {
        userRepository.saveAndFlush(newUser("repo-user@example.com"));

        assertThat(userRepository.findByEmail("repo-user@example.com")).isPresent();
        assertThat(userRepository.existsByEmail("repo-user@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void duplicateEmail_ShouldViolateUniqueConstraint() {
        userRepository.saveAndFlush(newUser("repo-dup@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("repo-dup@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
