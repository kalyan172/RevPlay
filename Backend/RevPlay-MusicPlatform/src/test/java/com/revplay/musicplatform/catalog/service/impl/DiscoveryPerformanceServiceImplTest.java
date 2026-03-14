package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DiscoveryPerformanceServiceImplTest {

    private static final String TABLE_CHECK_SQL = """
                SELECT COUNT(1)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """;
    private static final String INDEX_CHECK_SQL = """
                SELECT COUNT(1)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DiscoveryPerformanceServiceImpl service;

    @Test
    @DisplayName("ensureIndexes creates index when table exists and index missing")
    void ensureIndexesCreatesMissingIndex() {
        when(jdbcTemplate.queryForObject(eq(TABLE_CHECK_SQL), eq(Long.class), anyString())).thenReturn(1L);
        when(jdbcTemplate.queryForObject(eq(INDEX_CHECK_SQL), eq(Long.class), anyString(), anyString())).thenReturn(0L);

        service.ensureIndexes();

        verify(jdbcTemplate).execute("CREATE INDEX idx_songs_title ON songs(title)");
    }

    @Test
    @DisplayName("ensureIndexes skips execute when table does not exist")
    void ensureIndexesSkipsMissingTable() {
        when(jdbcTemplate.queryForObject(eq(TABLE_CHECK_SQL), eq(Long.class), eq("songs"))).thenReturn(0L);

        assertThatCode(() -> service.ensureIndexes()).doesNotThrowAnyException();

        verify(jdbcTemplate, never()).execute("CREATE INDEX idx_songs_title ON songs(title)");
    }
}
