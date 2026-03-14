package com.revplay.musicplatform.artist.contoller;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
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
import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkUpdateRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistSocialLinkResponse;
import com.revplay.musicplatform.artist.service.ArtistSocialLinkService;
import com.revplay.musicplatform.catalog.enums.SocialPlatform;
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
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArtistSocialLinkController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class ArtistSocialLinkControllerTest {

    private static final String BASE_PATH = "/api/v1/artists/1/social-links";
    private static final Long ARTIST_ID = 1L;
    private static final Long LINK_ID = 2L;
    private static final String ARTIST_USER = "artist";
    private static final String ROLE_ARTIST = "ARTIST";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private ArtistSocialLinkService service;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    ArtistSocialLinkControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
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
    @DisplayName("POST create social link with auth returns 201")
    void createWithAuth() throws Exception {
        when(service.create(any(Long.class), any(ArtistSocialLinkCreateRequest.class))).thenReturn(new ArtistSocialLinkResponse());

        mockMvc.perform(post(BASE_PATH)
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).create(any(Long.class), any(ArtistSocialLinkCreateRequest.class));
    }

    @Test
    @DisplayName("POST create social link invalid body returns 400")
    void createInvalidBody() throws Exception {
        ArtistSocialLinkCreateRequest request = new ArtistSocialLinkCreateRequest();
        request.setPlatform(SocialPlatform.INSTAGRAM);

        mockMvc.perform(post(BASE_PATH)
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET list social links with auth returns 200")
    void listWithAuth() throws Exception {
        when(service.list(ARTIST_ID)).thenReturn(List.of(new ArtistSocialLinkResponse()));

        mockMvc.perform(get(BASE_PATH).with(user(ARTIST_USER).roles(ROLE_ARTIST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).list(ARTIST_ID);
    }

    @Test
    @DisplayName("GET list social links without auth returns 403")
    void listNoAuth() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT update social link with auth returns 200")
    void updateWithAuth() throws Exception {
        when(service.update(any(Long.class), any(Long.class), any(ArtistSocialLinkUpdateRequest.class))).thenReturn(new ArtistSocialLinkResponse());

        mockMvc.perform(put(BASE_PATH + "/2")
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).update(any(Long.class), any(Long.class), any(ArtistSocialLinkUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT update social link not found returns 404")
    void updateNotFound() throws Exception {
        when(service.update(any(Long.class), any(Long.class), any(ArtistSocialLinkUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Social link not found"));

        mockMvc.perform(put(BASE_PATH + "/2")
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isNotFound());

        verify(service).update(any(Long.class), any(Long.class), any(ArtistSocialLinkUpdateRequest.class));
    }

    @Test
    @DisplayName("DELETE social link with auth returns 200")
    void deleteWithAuth() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/2")
                        .with(user(ARTIST_USER).roles(ROLE_ARTIST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).delete(ARTIST_ID, LINK_ID);
    }

    @Test
    @DisplayName("POST create social link without auth returns 403")
    void createNoAuth() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    private ArtistSocialLinkCreateRequest createRequest() {
        ArtistSocialLinkCreateRequest request = new ArtistSocialLinkCreateRequest();
        request.setPlatform(SocialPlatform.INSTAGRAM);
        request.setUrl("https://instagram.com/x");
        return request;
    }

    private ArtistSocialLinkUpdateRequest updateRequest() {
        ArtistSocialLinkUpdateRequest request = new ArtistSocialLinkUpdateRequest();
        request.setPlatform(SocialPlatform.YOUTUBE);
        request.setUrl("https://youtube.com/x");
        return request;
    }
}
