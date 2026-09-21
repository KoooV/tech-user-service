package com.kov.techuserservice.entity.repository;

import com.kov.techuserservice.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Query("select rt from RefreshToken rt where rt.user.id = :userId and rt.revoked = false and rt.expiresAt > current_timestamp")
    List<RefreshToken> findAllValidTokenByUserId(Long userId);

    void revokeAllByUserId(Long userId);

    void revokeByToken(String token);
}