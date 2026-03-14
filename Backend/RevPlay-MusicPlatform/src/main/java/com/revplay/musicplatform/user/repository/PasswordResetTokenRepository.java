package com.revplay.musicplatform.user.repository;

import com.revplay.musicplatform.user.entity.PasswordResetToken;
import com.revplay.musicplatform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);

    void deleteByExpiryDateBefore(Instant expiryTime);
}
