package com.revplay.musicplatform.ads.repository;

import com.revplay.musicplatform.ads.entity.UserAdPlaybackState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAdPlaybackStateRepository extends JpaRepository<UserAdPlaybackState, Long> {

    Optional<UserAdPlaybackState> findByUserId(Long userId);
}

