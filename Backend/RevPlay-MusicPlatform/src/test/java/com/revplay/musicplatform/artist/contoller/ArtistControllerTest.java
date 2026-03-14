package com.revplay.musicplatform.artist.contoller;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.artist.dto.request.ArtistCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistUpdateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistVerifyRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistResponse;
import com.revplay.musicplatform.artist.dto.response.ArtistSummaryResponse;
import com.revplay.musicplatform.artist.enums.ArtistType;
import com.revplay.musicplatform.artist.service.ArtistService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArtistController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class ArtistControllerTest {

    private static final String BASE_PATH = "/api/v1/artists";
    private static final Long ARTIST_ID = 1L;
    private static final String ARTIST_USER = "artist";
    private static final String ADMIN_USER = "admin";
    private static final String ROLE_ARTIST = "ARTIST";
    private static final String ROLE_ADMIN = "ADMIN";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private ArtistService artistService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    ArtistControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("POST create artist with auth returns 201")
    void createWithAuth() throws Exception {
        ArtistResponse response = new ArtistResponse();
        response.setArtistId(ARTIST_ID);
        when(artistService.createArtist(any(ArtistCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_PATH)
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(artistService).createArtist(any(ArtistCreateRequest.class));
    }

    @Test
    @DisplayName("POST create artist invalid body returns 400")
    void createInvalidBody() throws Exception {
        ArtistCreateRequest request = new ArtistCreateRequest();
        request.setBio("Bio only");

        mockMvc.perform(post(BASE_PATH)
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("POST create artist without auth returns 403")
    void createNoAuth() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("PUT update artist with auth returns 200")
    void updateWithAuth() throws Exception {
        when(artistService.updateArtist(any(Long.class), any(ArtistUpdateRequest.class))).thenReturn(new ArtistResponse());

        mockMvc.perform(put(BASE_PATH + "/1")
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(artistService).updateArtist(any(Long.class), any(ArtistUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT update artist not found returns 404")
    void updateNotFound() throws Exception {
        when(artistService.updateArtist(any(Long.class), any(ArtistUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Artist not found"));

        mockMvc.perform(put(BASE_PATH + "/1")
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(artistService).updateArtist(any(Long.class), any(ArtistUpdateRequest.class));
    }

    @Test
    @DisplayName("GET artist profile with auth returns 200")
    void getArtistWithAuth() throws Exception {
        when(artistService.getArtist(ARTIST_ID)).thenReturn(new ArtistResponse());

        mockMvc.perform(get(BASE_PATH + "/1").with(user(ARTIST_USER).roles(ROLE_ARTIST)))
                .andExpect(status().isOk());

        verify(artistService).getArtist(ARTIST_ID);
    }

    @Test
    @DisplayName("GET artist profile without auth returns 403")
    void getArtistNoAuth() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("PATCH verify artist with admin auth returns 200")
    void verifyWithAdmin() throws Exception {
        ArtistVerifyRequest request = new ArtistVerifyRequest();
        request.setVerified(Boolean.TRUE);
        when(artistService.verifyArtist(any(Long.class), any(ArtistVerifyRequest.class))).thenReturn(new ArtistResponse());

        mockMvc.perform(patch(BASE_PATH + "/1/verify")
                        .with(user(ADMIN_USER).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(artistService).verifyArtist(any(Long.class), any(ArtistVerifyRequest.class));
    }

    @Test
    @DisplayName("PATCH verify artist without auth returns 403")
    void verifyNoAuth() throws Exception {
        ArtistVerifyRequest request = new ArtistVerifyRequest();
        request.setVerified(Boolean.TRUE);

        mockMvc.perform(patch(BASE_PATH + "/1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("GET artist summary with auth returns 200")
    void summaryWithAuth() throws Exception {
        when(artistService.getSummary(ARTIST_ID)).thenReturn(new ArtistSummaryResponse());

        mockMvc.perform(get(BASE_PATH + "/1/summary").with(user(ARTIST_USER).roles(ROLE_ARTIST)))
                .andExpect(status().isOk());

        verify(artistService).getSummary(ARTIST_ID);
    }

    private ArtistCreateRequest createRequest() {
        ArtistCreateRequest request = new ArtistCreateRequest();
        request.setDisplayName("Name");
        request.setBio("Bio");
        request.setBannerImageUrl("banner");
        request.setArtistType(ArtistType.MUSIC);
        return request;
    }

    private ArtistUpdateRequest updateRequest() {
        ArtistUpdateRequest request = new ArtistUpdateRequest();
        request.setDisplayName("Updated");
        request.setBio("Updated bio");
        request.setBannerImageUrl("updated-banner");
        request.setArtistType(ArtistType.PODCAST);
        return request;
    }
}
