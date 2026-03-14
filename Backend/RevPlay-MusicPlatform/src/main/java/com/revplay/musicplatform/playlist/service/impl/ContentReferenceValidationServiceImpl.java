package com.revplay.musicplatform.playlist.service.impl;

import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.playlist.service.ContentReferenceValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class ContentReferenceValidationServiceImpl implements ContentReferenceValidationService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.likes.validate-content-existence:false}")
    private boolean validateContentExistence;

    public void validateLikeTargetExists(String likeableType, Long likeableId) {
        if (!validateContentExistence) {
            return;
        }

        String normalizedType = likeableType.toUpperCase();
        String sql;
        if ("SONG".equals(normalizedType)) {
            sql = "SELECT COUNT(1) FROM songs WHERE song_id = ?";
        } else if ("PODCAST".equals(normalizedType)) {
            sql = "SELECT COUNT(1) FROM podcasts WHERE podcast_id = ?";
        } else {
            throw new IllegalArgumentException("Unsupported likeableType: " + likeableType);
        }

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, likeableId);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException(normalizedType + " not found with id: " + likeableId);
        }

        log.debug("Validated like target exists: type={}, id={}", normalizedType, likeableId);
    }
}


