package com.revplay.musicplatform.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.catalog.dto.request.SongUpdateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongVisibilityRequest;
import com.revplay.musicplatform.catalog.dto.response.SongResponse;
import com.revplay.musicplatform.catalog.enums.ContentVisibility;
import com.revplay.musicplatform.catalog.service.SongService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SongController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class SongControllerTest {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    @MockBean private SongService songService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private FileStorageProperties fileStorageProperties;
    @MockBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    SongControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) { this.mockMvc = mockMvc; this.objectMapper = objectMapper; }

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0, ServletRequest.class), invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(ServletRequest.class), org.mockito.ArgumentMatchers.any(ServletResponse.class), org.mockito.ArgumentMatchers.any(FilterChain.class));
    }

    @Test
    @DisplayName("create song returns 201")
    void createSongReturns201() throws Exception {
        when(songService.create(any(), any())).thenReturn(songResponse());
        MockMultipartFile metadata = new MockMultipartFile("metadata", "", "application/json", "{\"title\":\"Song\",\"durationSeconds\":180}".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "song.mp3", "audio/mpeg", "abc".getBytes());
        mockMvc.perform(multipart("/api/v1/songs").file(metadata).file(file).with(user("artist").roles("ARTIST"))).andExpect(status().isCreated()).andExpect(jsonPath("$.data.songId").value(1));
    }

    @Test
    @DisplayName("invalid song metadata returns 400")
    void invalidSongMetadataReturns400() throws Exception {
        MockMultipartFile metadata = new MockMultipartFile("metadata", "", "application/json", "{".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "song.mp3", "audio/mpeg", "abc".getBytes());
        mockMvc.perform(multipart("/api/v1/songs").file(metadata).file(file).with(user("artist").roles("ARTIST"))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("get missing song returns 404")
    void getMissingSongReturns404() throws Exception {
        when(songService.get(7L)).thenThrow(new ResourceNotFoundException("Song not found"));
        mockMvc.perform(get("/api/v1/songs/7").with(user("artist").roles("ARTIST"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("list by artist returns page")
    void listByArtistReturnsPage() throws Exception {
        when(songService.listByArtist(eq(4L), any())).thenReturn(new PageImpl<>(List.of(songResponse()), PageRequest.of(0, 20), 1));
        mockMvc.perform(get("/api/v1/artists/4/songs").with(user("artist").roles("ARTIST"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].songId").value(1));
    }

    @Test
    @DisplayName("update visibility returns 200")
    void updateVisibilityReturns200() throws Exception {
        when(songService.updateVisibility(eq(1L), any())).thenReturn(songResponse());
        SongVisibilityRequest request = new SongVisibilityRequest();
        request.setVisibility(ContentVisibility.PUBLIC);
        mockMvc.perform(patch("/api/v1/songs/1/visibility").with(user("artist").roles("ARTIST")).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Song visibility updated"));
    }

    @Test
    @DisplayName("replace audio returns 200")
    void replaceAudioReturns200() throws Exception {
        when(songService.replaceAudio(eq(1L), any())).thenReturn(songResponse());
        MockMultipartFile file = new MockMultipartFile("file", "song.mp3", "audio/mpeg", "abc".getBytes());
        mockMvc.perform(multipart("/api/v1/songs/1/audio").file(file).with(request -> { request.setMethod("PUT"); return request; }).with(user("artist").roles("ARTIST"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.songId").value(1));
    }

    @Test
    @DisplayName("update song invalid body returns 400 in current validation")
    void updateSongReturns400ForMissingDuration() throws Exception {
        SongUpdateRequest request = new SongUpdateRequest();
        request.setTitle("Updated");
        mockMvc.perform(put("/api/v1/songs/1").with(user("artist").roles("ARTIST")).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("delete song returns 200")
    void deleteSongReturns200() throws Exception {
        mockMvc.perform(delete("/api/v1/songs/1").with(user("artist").roles("ARTIST"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Song deleted"));
    }

    private SongResponse songResponse() {
        SongResponse response = new SongResponse();
        response.setSongId(1L);
        response.setArtistId(2L);
        response.setTitle("Song");
        response.setVisibility(ContentVisibility.PUBLIC);
        return response;
    }
}
