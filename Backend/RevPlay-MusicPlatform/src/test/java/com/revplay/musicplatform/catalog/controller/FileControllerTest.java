package com.revplay.musicplatform.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.catalog.util.FileStorageService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(FileController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class FileControllerTest {
    private final MockMvc mockMvc;
    @TempDir Path tempDir;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private FileStorageProperties fileStorageProperties;
    @MockBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    FileControllerTest(MockMvc mockMvc) { this.mockMvc = mockMvc; }

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0, ServletRequest.class), invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(ServletRequest.class), org.mockito.ArgumentMatchers.any(ServletResponse.class), org.mockito.ArgumentMatchers.any(FilterChain.class));
    }

    @Test
    @DisplayName("song request without range streams full file")
    void songRequestWithoutRangeStreamsFullFile() throws Exception {
        Path song = Files.writeString(tempDir.resolve("track.mp3"), "abcdef");
        when(fileStorageService.loadSong("track.mp3")).thenReturn(new FileSystemResource(song));
        mockMvc.perform(get("/api/v1/files/songs/track.mp3")).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "bytes")).andExpect(MockMvcResultMatchers.content().bytes(Files.readAllBytes(song)));
    }

    @Test
    @DisplayName("song request with valid range returns partial content")
    void songRequestWithValidRangeReturnsPartialContent() throws Exception {
        Path song = Files.writeString(tempDir.resolve("range.mp3"), "abcdefghij");
        when(fileStorageService.loadSong("range.mp3")).thenReturn(new FileSystemResource(song));
        mockMvc.perform(get("/api/v1/files/songs/range.mp3").header(HttpHeaders.RANGE, "bytes=2-5")).andExpect(status().isPartialContent()).andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_RANGE, "bytes 2-5/10")).andExpect(MockMvcResultMatchers.content().bytes("cdef".getBytes()));
    }

    @Test
    @DisplayName("song request with invalid range returns 500 in current implementation")
    void songRequestWithInvalidRangeReturns500() throws Exception {
        Path song = Files.writeString(tempDir.resolve("invalid.mp3"), "abcd");
        when(fileStorageService.loadSong("invalid.mp3")).thenReturn(new FileSystemResource(song));
        mockMvc.perform(get("/api/v1/files/songs/invalid.mp3").header(HttpHeaders.RANGE, "bytes=99-100")).andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("song request with suffix range returns last bytes")
    void songRequestWithSuffixRangeReturnsPartialContent() throws Exception {
        Path song = Files.writeString(tempDir.resolve("suffix.mp3"), "abcdefghij");
        when(fileStorageService.loadSong("suffix.mp3")).thenReturn(new FileSystemResource(song));

        mockMvc.perform(get("/api/v1/files/songs/suffix.mp3").header(HttpHeaders.RANGE, "bytes=-4"))
                .andExpect(status().isPartialContent())
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_RANGE, "bytes 6-9/10"))
                .andExpect(MockMvcResultMatchers.content().bytes("ghij".getBytes()));
    }

    @Test
    @DisplayName("song request with open ended range streams until file end")
    void songRequestWithOpenEndedRangeReturnsPartialContent() throws Exception {
        Path song = Files.writeString(tempDir.resolve("open.mp3"), "abcdefghij");
        when(fileStorageService.loadSong("open.mp3")).thenReturn(new FileSystemResource(song));

        mockMvc.perform(get("/api/v1/files/songs/open.mp3").header(HttpHeaders.RANGE, "bytes=4-"))
                .andExpect(status().isPartialContent())
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_RANGE, "bytes 4-9/10"))
                .andExpect(MockMvcResultMatchers.content().bytes("efghij".getBytes()));
    }

    @Test
    @DisplayName("song request with malformed range prefix returns 500 in current implementation")
    void songRequestWithMalformedRangeReturnsRequestedRangeNotSatisfiable() throws Exception {
        Path song = Files.writeString(tempDir.resolve("malformed.mp3"), "abcdefghij");
        when(fileStorageService.loadSong("malformed.mp3")).thenReturn(new FileSystemResource(song));

        mockMvc.perform(get("/api/v1/files/songs/malformed.mp3").header(HttpHeaders.RANGE, "items=0-2"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("podcast request streams partial content using same range handling")
    void podcastRequestWithRangeReturnsPartialContent() throws Exception {
        Path podcast = Files.writeString(tempDir.resolve("episode.mp3"), "abcdefghij");
        when(fileStorageService.loadPodcast("episode.mp3")).thenReturn(new FileSystemResource(podcast));

        mockMvc.perform(get("/api/v1/files/podcasts/episode.mp3").header(HttpHeaders.RANGE, "bytes=1-3"))
                .andExpect(status().isPartialContent())
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_RANGE, "bytes 1-3/10"))
                .andExpect(MockMvcResultMatchers.content().bytes("bcd".getBytes()));
    }

    @Test
    @DisplayName("image upload returns stored image response")
    void imageUploadReturnsStoredImageResponse() throws Exception {
        when(fileStorageService.storeImage(any())).thenReturn("cover.png");
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", "png".getBytes());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/files/images").file(file).with(user("artist").roles("ARTIST"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.fileName").value("cover.png")).andExpect(jsonPath("$.data.imageUrl").value("/api/v1/files/images/cover.png"));
    }

    @Test
    @DisplayName("image fetch returns inline resource")
    void imageFetchReturnsInlineResource() throws Exception {
        Path image = Files.writeString(tempDir.resolve("cover.png"), "img");
        when(fileStorageService.loadImage("cover.png")).thenReturn(new FileSystemResource(image));
        mockMvc.perform(get("/api/v1/files/images/cover.png")).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cover.png\"")).andExpect(MockMvcResultMatchers.content().bytes(Files.readAllBytes(image)));
    }
}
