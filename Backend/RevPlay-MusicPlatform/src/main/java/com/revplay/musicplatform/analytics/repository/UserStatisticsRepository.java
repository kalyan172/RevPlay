package com.revplay.musicplatform.analytics.repository;

import com.revplay.musicplatform.analytics.entity.UserStatistics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

    Optional<UserStatistics> findByUserId(Long userId);
}




