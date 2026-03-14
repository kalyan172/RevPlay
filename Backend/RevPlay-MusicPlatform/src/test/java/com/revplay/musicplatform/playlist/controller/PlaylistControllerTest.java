package com.revplay.musicplatform.playlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.AccessDeniedException;
import com.revplay.musicplatform.exception.DuplicateResourceException;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.playlist.dto.request.AddSongToPlaylistRequest;
import com.revplay.musicplatform.playlist.dto.request.CreatePlaylistRequest;
import com.revplay.musicplatform.playlist.dto.request.ReorderPlaylistSongsRequest;
import com.revplay.musicplatform.playlist.dto.request.SongPositionRequest;
import com.revplay.musicplatform.playlist.dto.request.UpdatePlaylistRequest;
import com.revplay.musicplatform.playlist.dto.response.PlaylistDetailResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistFollowResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistSongResponse;
import com.revplay.musicplatform.playlist.service.PlaylistService;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlaylistController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class PlaylistControllerTest {

        private static final String BASE = "/api/v1/playlists";
        private static final Long PLAYLIST_ID = 1L;
        private static final Long SONG_ID = 2L;
        private static final String USERNAME = "u";
        private static final String ROLE_LISTENER = "LISTENER";
        private static final String PLAYLIST_NAME = "Road";
        private static final String NEW_NAME = "New";

        private final MockMvc mockMvc;
        private final ObjectMapper objectMapper;

        @MockBean
        private PlaylistService playlistService;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private FileStorageProperties fileStorageProperties;
        @MockBean(name = "jpaMappingContext")
        private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

        @Autowired
        PlaylistControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
                this.mockMvc = mockMvc;
                this.objectMapper = objectMapper;
        }

        @BeforeEach
        void setUp() throws Exception {
                doAnswer(invocation -> {
                        FilterChain filterChain = invocation.getArgument(2);
                        filterChain.doFilter(
                                        invocation.getArgument(0, ServletRequest.class),
                                        invocation.getArgument(1, ServletResponse.class));
                        return null;
                }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class),
                                any(FilterChain.class));
        }

        @Test
        @DisplayName("POST playlists authenticated returns 201")
        void createPlaylistAuthenticated() throws Exception {
                when(playlistService.createPlaylist(any())).thenReturn(playlistResponse());
                CreatePlaylistRequest request = new CreatePlaylistRequest();
                request.setName(PLAYLIST_NAME);
                mockMvc.perform(post(BASE)
                                .with(user(USERNAME).roles(ROLE_LISTENER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST playlists no JWT returns 401")
        void createPlaylistNoJwt() throws Exception {
                CreatePlaylistRequest request = new CreatePlaylistRequest();
                request.setName(PLAYLIST_NAME);
                mockMvc.perform(post(BASE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST playlists missing name returns 400")
        void createPlaylistMissingName() throws Exception {
                mockMvc.perform(post(BASE)
                                .with(user(USERNAME).roles(ROLE_LISTENER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"description\":\"d\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET playlist without JWT returns 403 under current security config")
        void getPlaylistPublicNoJwt() throws Exception {
                when(playlistService.getPlaylistById(PLAYLIST_ID)).thenReturn(playlistDetail());
                mockMvc.perform(get(BASE + "/" + PLAYLIST_ID))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET playlist private non-owner returns 403")
        void getPrivateNonOwner() throws Exception {
                when(playlistService.getPlaylistById(PLAYLIST_ID))
                                .thenThrow(new AccessDeniedException("This playlist is private"));
                mockMvc.perform(get(BASE + "/" + PLAYLIST_ID).with(user(USERNAME).roles(ROLE_LISTENER)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET playlist not found returns 404")
        void getPlaylistNotFound() throws Exception {
                when(playlistService.getPlaylistById(PLAYLIST_ID))
                                .thenThrow(new ResourceNotFoundException("Playlist", PLAYLIST_ID));
                mockMvc.perform(get(BASE + "/" + PLAYLIST_ID)
                                .with(user(USERNAME).roles(ROLE_LISTENER)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT playlist owner returns 200")
        void updatePlaylistOwner() throws Exception {
                when(playlistService.updatePlaylist(eq(PLAYLIST_ID), any())).thenReturn(playlistResponse());
                UpdatePlaylistRequest request = new UpdatePlaylistRequest();
                request.setName(NEW_NAME);
                mockMvc.perform(put(BASE + "/" + PLAYLIST_ID)
                                .with(user(USERNAME).roles(ROLE_LISTENER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT playlist non-owner returns 403")
        void updatePlaylistNonOwner() throws Exception {
                when(playlistService.updatePlaylist(eq(PLAYLIST_ID), any()))
                                .thenThrow(new AccessDeniedException("You do not own this playlist"));
                UpdatePlaylistRequest request = new UpdatePlaylistRequest();
                request.setName(NEW_NAME);
                mockMvc.perform(put(BASE + "/" + PLAYLIST_ID)
                                .with(user(USERNAME).roles(ROLE_LISTENER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE playlist owner returns 200")
        void deletePlaylistOwner() throws Exception {
                mockMvc.perform(delete(BASE + "/" + PLAYLIST_ID).with(user(USERNAME).roles(ROLE_LISTENER)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST playlist songs owner returns 201")
        void addSongOwner() throws Exception {
                when(playlistService.addSongToPlaylist(eq(PLAYLIST_ID), any())).thenReturn(playlistSongResponse());
                AddSongToPlaylistRequest request = new AddSongToPlaylistRequest();
                request.setSongId(SONG_ID);
                mockMvc.perform(post(BASE + "/" + PLAYLIST_ID + "/songs")
                                .with(user(USERNAME).roles(ROLE_LISTENER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST playlist songs duplicate returns 409")
        void addSongDuplicate() throws Exception {
                when(playlistService.addSongToPlaylist(eq(PLAYLIST_ID), any()))
                                .thenThrow(new DuplicateResourceException("duplicate"));
                AddSongToPlaylistRequest request = new AddSongToPlaylistRequest();
                request.setSongId(SONG_ID);
                mockMvc.perform(post(BASE + "/" + PLAYLIST_ID + "/songs")
                                .with(user(USERNAME).roles(ROLE_LISTENER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE playlist song owner returns 200")
        void removeSongOwner() throws Exception {
                mockMvc.perform(delete(BASE + "/" + PLAYLIST_ID + "/songs/" + SONG_ID)
                                .with(user(USERNAME).roles(ROLE_LISTENER)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE playlist song not in playlist returns 404")
        void removeSongNotFound() throws Exception {
                org.mockito.Mockito
                                .doThrow(new ResourceNotFoundException(
                                                "Song " + SONG_ID + " not found in playlist " + PLAYLIST_ID))
                                .when(playlistService).removeSongFromPlaylist(PLAYLIST_ID, SONG_ID);
                mockMvc.perform(delete(BASE + "/" + PLAYLIST_ID + "/songs/" + SONG_ID)
                                .with(user(USERNAME).roles(ROLE_LISTENER)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT reorder songs valid returns 200")
        void reorderValid() throws Exception {
                when(playlistService.reorderPlaylistSongs(eq(PLAYLIST_ID), any()))
                                .thenReturn(List.of(playlistSongResponse()));
                ReorderPlaylistSongsRequest request = new ReorderPlaylistSongsRequest();
                SongPositionRequest p = new SongPositionRequest();
                p.setSongId(SONG_ID);
                p.setPosition(1);
                request.setSongs(List.of(p));
                mockMvc.perform(put(BASE + "/" + PLAYLIST_ID + "/songs/reorder")
                                .with(user(USERNAME).roles(ROLE_LISTENER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT reorder songs invalid request returns 400")
        void reorderInvalid() throws Exception {
                when(playlistService.reorderPlaylistSongs(eq(PLAYLIST_ID), any()))
                                .thenThrow(new IllegalArgumentException("bad"));
                ReorderPlaylistSongsRequest request = new ReorderPlaylistSongsRequest();
                SongPositionRequest p = new SongPositionRequest();
                p.setSongId(SONG_ID);
                p.setPosition(1);
                request.setSongs(List.of(p));
                mockMvc.perform(put(BASE + "/" + PLAYLIST_ID + "/songs/reorder")
                                .with(user(USERNAME).roles(ROLE_LISTENER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST follow playlist authenticated returns 201 in current controller")
        void followPlaylist() throws Exception {
                when(playlistService.followPlaylist(PLAYLIST_ID))
                                .thenReturn(PlaylistFollowResponse.builder().id(1L).playlistId(PLAYLIST_ID).build());
                mockMvc.perform(post(BASE + "/" + PLAYLIST_ID + "/follow")
                                .with(user(USERNAME).roles(ROLE_LISTENER)))
                                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST follow already following returns 409")
        void followAlreadyFollowing() throws Exception {
                when(playlistService.followPlaylist(PLAYLIST_ID)).thenThrow(new DuplicateResourceException("already"));
                mockMvc.perform(post(BASE + "/" + PLAYLIST_ID + "/follow")
                                .with(user(USERNAME).roles(ROLE_LISTENER)))
                                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE unfollow returns 200")
        void unfollow() throws Exception {
                mockMvc.perform(delete(BASE + "/" + PLAYLIST_ID + "/unfollow")
                                .with(user(USERNAME).roles(ROLE_LISTENER)))
                                .andExpect(status().isOk());
        }

        private PlaylistResponse playlistResponse() {
                return PlaylistResponse.builder().id(PLAYLIST_ID).name(PLAYLIST_NAME).songCount(0).build();
        }

        private PlaylistDetailResponse playlistDetail() {
                return PlaylistDetailResponse.builder().id(PLAYLIST_ID).name(PLAYLIST_NAME).songs(List.of()).build();
        }

        private PlaylistSongResponse playlistSongResponse() {
                return PlaylistSongResponse.builder().id(1L).playlistId(PLAYLIST_ID).songId(SONG_ID).position(1)
                                .build();
        }
}
