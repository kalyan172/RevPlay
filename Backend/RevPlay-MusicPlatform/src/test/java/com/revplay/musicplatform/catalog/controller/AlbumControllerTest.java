package com.revplay.musicplatform.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.catalog.dto.request.AlbumCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.AlbumUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.AlbumResponse;
import com.revplay.musicplatform.catalog.service.AlbumService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlbumController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class AlbumControllerTest {

    private static final String ALBUMS_PATH = "/api/v1/albums";
    private static final String ARTISTS_PATH = "/api/v1/artists";
    private static final String TITLE = "Album One";
    private static final Long ALBUM_ID = 10L;
    private static final Long ARTIST_ID = 20L;
    private static final String ARTIST_USER = "artist";
    private static final String ARTIST_ROLE = "ARTIST";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private AlbumService albumService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    AlbumControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
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
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    @DisplayName("create album with auth returns 201")
    void createWithAuthReturns201() throws Exception {
        AlbumCreateRequest request = new AlbumCreateRequest();
        request.setTitle(TITLE);
        AlbumResponse response = albumResponse();
        when(albumService.create(any(AlbumCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post(ALBUMS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(user(ARTIST_USER).roles(ARTIST_ROLE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.albumId").value(ALBUM_ID));

        verify(albumService).create(any(AlbumCreateRequest.class));
    }

    @Test
    @DisplayName("create album without auth returns 403")
    void createWithoutAuthReturns403() throws Exception {
        AlbumCreateRequest request = new AlbumCreateRequest();
        request.setTitle(TITLE);

        mockMvc.perform(post(ALBUMS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(albumService);
    }

    @Test
    @DisplayName("create album missing title returns 400")
    void createMissingTitleReturns400() throws Exception {
        AlbumCreateRequest request = new AlbumCreateRequest();

        mockMvc.perform(post(ALBUMS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(user(ARTIST_USER).roles(ARTIST_ROLE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(albumService);
    }

    @Test
    @DisplayName("update album with auth returns 200")
    void updateWithAuthReturns200() throws Exception {
        AlbumUpdateRequest request = new AlbumUpdateRequest();
        request.setTitle("Updated");
        when(albumService.update(eq(ALBUM_ID), any(AlbumUpdateRequest.class))).thenReturn(albumResponse());

        mockMvc.perform(put(ALBUMS_PATH + "/" + ALBUM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(user(ARTIST_USER).roles(ARTIST_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(albumService).update(eq(ALBUM_ID), any(AlbumUpdateRequest.class));
    }

    @Test
    @DisplayName("get album returns 200")
    void getReturns200() throws Exception {
        when(albumService.get(ALBUM_ID)).thenReturn(albumResponse());

        mockMvc.perform(get(ALBUMS_PATH + "/" + ALBUM_ID)
                .with(user(ARTIST_USER).roles(ARTIST_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.albumId").value(ALBUM_ID));

        verify(albumService).get(ALBUM_ID);
    }

    @Test
    @DisplayName("get album not found returns 404")
    void getNotFoundReturns404() throws Exception {
        when(albumService.get(ALBUM_ID)).thenThrow(new ResourceNotFoundException("Album not found"));

        mockMvc.perform(get(ALBUMS_PATH + "/" + ALBUM_ID)
                .with(user(ARTIST_USER).roles(ARTIST_ROLE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(albumService).get(ALBUM_ID);
    }

    @Test
    @DisplayName("delete album with auth returns 200")
    void deleteWithAuthReturns200() throws Exception {
        mockMvc.perform(delete(ALBUMS_PATH + "/" + ALBUM_ID)
                .with(user(ARTIST_USER).roles(ARTIST_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(albumService).delete(ALBUM_ID);
    }

    @Test
    @DisplayName("list by artist returns 200")
    void listByArtistReturns200() throws Exception {
        when(albumService.listByArtist(eq(ARTIST_ID), any()))
                .thenReturn(new PageImpl<>(List.of(albumResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(ARTISTS_PATH + "/" + ARTIST_ID + "/albums")
                .with(user(ARTIST_USER).roles(ARTIST_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].albumId").value(ALBUM_ID));

        verify(albumService).listByArtist(eq(ARTIST_ID), any());
    }

    private AlbumResponse albumResponse() {
        AlbumResponse response = new AlbumResponse();
        response.setAlbumId(ALBUM_ID);
        response.setArtistId(ARTIST_ID);
        response.setTitle(TITLE);
        response.setReleaseDate(LocalDate.now());
        return response;
    }
}
