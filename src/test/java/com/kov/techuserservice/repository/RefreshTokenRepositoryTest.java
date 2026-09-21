package com.kov.techuserservice.repository;

import com.kov.techuserservice.TestcontainersConfiguration;
import com.kov.techuserservice.entity.RefreshToken;
import com.kov.techuserservice.entity.User;
import com.kov.techuserservice.entity.repository.RefreshTokenRepository;
import com.kov.techuserservice.entity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Репозиторные тесты refresh-токенов: valid-выборка, отзыв, явное удаление
 * при {@code deleteUser} (детерминированный путь вместо надежды на FK-каскад).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser(String email) {
        User user = new User();
        user.setFirstName("Token");
        user.setLastName("Owner");
        user.setEmail(email);
        user.setPassword("$2a$10$hashed");
        user.setPhone("+1234567890");
        user.setActive(true);
        return userRepository.saveAndFlush(user);
    }

    private RefreshToken newToken(User user, String value, boolean revoked, Instant expiresAt) {
        return RefreshToken.builder()
                .user(user)
                .token(value)
                .tokenVersion(0L)
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();
    }

    @Test
    void findAllValidTokenByUserId_ShouldReturnOnlyLiveTokens() {
        User user = savedUser("rt-valid@example.com");
        Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        refreshTokenRepository.saveAllAndFlush(java.util.List.of(
                newToken(user, "live-token", false, future),
                newToken(user, "revoked-token", true, future),
                newToken(user, "expired-token", false, past)));

        var valid = refreshTokenRepository.findAllValidTokenByUserId(user.getId());

        assertThat(valid).extracting(RefreshToken::getToken).containsExactly("live-token");
    }

    @Test
    void revokeAllByUserId_ShouldInvalidateLiveTokens() {
        User user = savedUser("rt-revoke@example.com");
        refreshTokenRepository.saveAndFlush(
                newToken(user, "revoke-me", false, Instant.now().plus(1, ChronoUnit.DAYS)));

        refreshTokenRepository.revokeAllByUserId(user.getId());

        assertThat(refreshTokenRepository.findAllValidTokenByUserId(user.getId())).isEmpty();
        assertThat(refreshTokenRepository.findByToken("revoke-me")).isPresent();
    }

    @Test
    void deleteByUserId_ShouldRemoveTokensSoUserCanBeDeleted() {
        User user = savedUser("rt-delete@example.com");
        refreshTokenRepository.saveAndFlush(
                newToken(user, "orphan-token", false, Instant.now().plus(1, ChronoUnit.DAYS)));

        // Путь приложения при deleteUser: сначала явное удаление токенов.
        refreshTokenRepository.deleteByUser_Id(user.getId());

        assertThat(refreshTokenRepository.findByToken("orphan-token")).isEmpty();
        userRepository.delete(user);
        userRepository.flush();
        assertThat(userRepository.findByEmail("rt-delete@example.com")).isEmpty();
    }
}
