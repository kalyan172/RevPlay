package com.revplay.musicplatform.playback.repository;

import com.revplay.musicplatform.playback.entity.PlayHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {

    List<PlayHistory> findByUserIdOrderByPlayedAtDescPlayIdDesc(Long userId);

    List<PlayHistory> findByUserIdOrderByPlayedAtDescPlayIdDesc(Long userId, Pageable pageable);

    long deleteByUserId(Long userId);
}



