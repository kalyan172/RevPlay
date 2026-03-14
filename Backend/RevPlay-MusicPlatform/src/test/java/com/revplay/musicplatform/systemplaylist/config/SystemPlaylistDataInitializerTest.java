package com.revplay.musicplatform.systemplaylist.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.systemplaylist.entity.SystemPlaylist;
import com.revplay.musicplatform.systemplaylist.repository.SystemPlaylistRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@Tag("unit")
class SystemPlaylistDataInitializerTest {

    private static final long NON_ZERO_COUNT = 1L;
    private static final long ZERO_COUNT = 0L;
    private static final int EXPECTED_DEFAULT_PLAYLISTS = 5;
    private static final String TELUGU_SLUG = "telugu-mix";
    private static final String TAMIL_SLUG = "tamil-mix";
    private static final String HINDI_SLUG = "hindi-mix";
    private static final String ENGLISH_SLUG = "english-mix";
    private static final String DJ_SLUG = "dj-mix";

    @Test
    @DisplayName("run does nothing when playlists already exist")
    void runSkipsSeedingWhenRepositoryHasData() throws Exception {
        SystemPlaylistRepository repository = Mockito.mock(SystemPlaylistRepository.class);
        when(repository.count()).thenReturn(NON_ZERO_COUNT);
        SystemPlaylistDataInitializer initializer = new SystemPlaylistDataInitializer(repository);

        initializer.run();

        verify(repository, never()).save(any(SystemPlaylist.class));
    }

    @Test
    @DisplayName("run seeds all default playlists when repository is empty")
    void runSeedsAllDefaultsWhenEmpty() throws Exception {
        SystemPlaylistRepository repository = Mockito.mock(SystemPlaylistRepository.class);
        when(repository.count()).thenReturn(ZERO_COUNT);
        when(repository.findBySlugAndDeletedAtIsNull(any())).thenReturn(Optional.empty());
        SystemPlaylistDataInitializer initializer = new SystemPlaylistDataInitializer(repository);

        initializer.run();

        ArgumentCaptor<SystemPlaylist> captor = ArgumentCaptor.forClass(SystemPlaylist.class);
        verify(repository).findBySlugAndDeletedAtIsNull(TELUGU_SLUG);
        verify(repository).findBySlugAndDeletedAtIsNull(TAMIL_SLUG);
        verify(repository).findBySlugAndDeletedAtIsNull(HINDI_SLUG);
        verify(repository).findBySlugAndDeletedAtIsNull(ENGLISH_SLUG);
        verify(repository).findBySlugAndDeletedAtIsNull(DJ_SLUG);
        verify(repository, org.mockito.Mockito.times(EXPECTED_DEFAULT_PLAYLISTS)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(playlist -> Boolean.TRUE.equals(playlist.getIsActive()));
    }

    @Test
    @DisplayName("run skips save when default slug already exists")
    void runSkipsSaveForExistingDefaultSlug() throws Exception {
        SystemPlaylistRepository repository = Mockito.mock(SystemPlaylistRepository.class);
        when(repository.count()).thenReturn(ZERO_COUNT);
        when(repository.findBySlugAndDeletedAtIsNull(TELUGU_SLUG)).thenReturn(Optional.of(new SystemPlaylist()));
        when(repository.findBySlugAndDeletedAtIsNull(TAMIL_SLUG)).thenReturn(Optional.empty());
        when(repository.findBySlugAndDeletedAtIsNull(HINDI_SLUG)).thenReturn(Optional.empty());
        when(repository.findBySlugAndDeletedAtIsNull(ENGLISH_SLUG)).thenReturn(Optional.empty());
        when(repository.findBySlugAndDeletedAtIsNull(DJ_SLUG)).thenReturn(Optional.empty());
        SystemPlaylistDataInitializer initializer = new SystemPlaylistDataInitializer(repository);

        initializer.run();

        verify(repository, org.mockito.Mockito.times(EXPECTED_DEFAULT_PLAYLISTS - 1)).save(any(SystemPlaylist.class));
    }
}
