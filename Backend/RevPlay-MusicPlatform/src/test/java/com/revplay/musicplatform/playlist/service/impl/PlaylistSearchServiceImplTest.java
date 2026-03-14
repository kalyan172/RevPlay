package com.revplay.musicplatform.playlist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.entity.Playlist;
import com.revplay.musicplatform.playlist.mapper.PlaylistMapper;
import com.revplay.musicplatform.playlist.repository.PlaylistFollowRepository;
import com.revplay.musicplatform.playlist.repository.PlaylistRepository;
import com.revplay.musicplatform.playlist.repository.PlaylistSongRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PlaylistSearchServiceImplTest {

    @Mock
    private PlaylistRepository playlistRepository;
    @Mock
    private PlaylistSongRepository playlistSongRepository;
    @Mock
    private PlaylistFollowRepository playlistFollowRepository;
    @Mock
    private PlaylistMapper playlistMapper;

    @InjectMocks
    private PlaylistSearchServiceImpl playlistSearchService;

    @Test
    @DisplayName("searchPublicPlaylists throws bad request when keyword blank")
    void searchBlankKeyword() {
        assertThatThrownBy(() -> playlistSearchService.searchPublicPlaylists(" ", 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("keyword is required");
    }

    @Test
    @DisplayName("searchPublicPlaylists throws bad request when page negative")
    void searchNegativePage() {
        assertThatThrownBy(() -> playlistSearchService.searchPublicPlaylists("rock", -1, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("page must be >= 0");
    }

    @Test
    @DisplayName("searchPublicPlaylists throws bad request when size out of range")
    void searchInvalidSize() {
        assertThatThrownBy(() -> playlistSearchService.searchPublicPlaylists("rock", 0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("size must be between 1 and 100");
        assertThatThrownBy(() -> playlistSearchService.searchPublicPlaylists("rock", 0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("size must be between 1 and 100");
    }

    @Test
    @DisplayName("searchPublicPlaylists trims keyword maps counts and returns paged response")
    void searchSuccess() {
        Playlist playlist = playlist(1L, 30L, "Road Trip");
        PageRequest pageable = PageRequest.of(0, 10);
        when(playlistRepository.searchPublicPlaylistsByKeyword(eq("rock"), eq(pageable)))
                .thenReturn(new PageImpl<>(java.util.List.of(playlist), pageable, 1));
        when(playlistSongRepository.countByPlaylistId(1L)).thenReturn(7L);
        when(playlistFollowRepository.countByPlaylistId(1L)).thenReturn(3L);
        when(playlistMapper.toResponse(playlist, 7L, 3L)).thenReturn(
                PlaylistResponse.builder().id(1L).name("Road Trip").songCount(7L).followerCount(3L).build()
        );

        PagedResponseDto<PlaylistResponse> response = playlistSearchService.searchPublicPlaylists("  rock  ", 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getSongCount()).isEqualTo(7L);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        verify(playlistRepository).searchPublicPlaylistsByKeyword("rock", pageable);
    }

    private Playlist playlist(Long id, Long userId, String name) {
        return Playlist.builder()
                .id(id)
                .userId(userId)
                .name(name)
                .description("desc")
                .isPublic(Boolean.TRUE)
                .isActive(Boolean.TRUE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }
}
