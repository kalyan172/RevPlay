package com.revplay.musicplatform.artist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.artist.dto.request.ArtistCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistUpdateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistVerifyRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistResponse;
import com.revplay.musicplatform.artist.dto.response.ArtistSummaryResponse;
import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.enums.ArtistType;
import com.revplay.musicplatform.artist.mapper.ArtistMapper;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.catalog.repository.AlbumRepository;
import com.revplay.musicplatform.catalog.repository.PodcastRepository;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import com.revplay.musicplatform.exception.ConflictException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ArtistServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;
    private static final Long ARTIST_ID = 100L;
    private static final String ROLE_ARTIST = "ARTIST";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ALREADY_EXISTS = "Artist profile already exists";
    private static final String NOT_FOUND = "Artist not found";

    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ArtistMapper artistMapper;
    @Mock
    private SongRepository songRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private PodcastRepository podcastRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AccessValidator accessValidator;

    private ArtistServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArtistServiceImpl(
                artistRepository,
                artistMapper,
                songRepository,
                albumRepository,
                podcastRepository,
                securityUtil,
                accessValidator);
    }

    @Test
    @DisplayName("createArtist saves when user has no artist profile")
    void createArtistSuccess() {
        ArtistCreateRequest request = createRequest();
        Artist entity = artist(USER_ID);
        Artist saved = artist(USER_ID);
        ArtistResponse mapped = new ArtistResponse();
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(artistRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(artistMapper.toEntity(request, USER_ID)).thenReturn(entity);
        when(artistRepository.save(entity)).thenReturn(saved);
        when(artistMapper.toResponse(saved)).thenReturn(mapped);

        ArtistResponse actual = service.createArtist(request);

        assertThat(actual).isSameAs(mapped);
        verify(accessValidator).requireArtistOrAdmin(ROLE_ARTIST);
    }

    @Test
    @DisplayName("createArtist throws conflict when profile already exists")
    void createArtistConflict() {
        ArtistCreateRequest request = createRequest();
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(artistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(artist(USER_ID)));

        assertThatThrownBy(() -> service.createArtist(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ALREADY_EXISTS);
    }

    @Test
    @DisplayName("updateArtist succeeds for owner")
    void updateArtistOwner() {
        ArtistUpdateRequest request = updateRequest();
        Artist artist = artist(USER_ID);
        ArtistResponse mapped = new ArtistResponse();
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(artistRepository.save(artist)).thenReturn(artist);
        when(artistMapper.toResponse(artist)).thenReturn(mapped);

        ArtistResponse actual = service.updateArtist(ARTIST_ID, request);

        assertThat(actual).isSameAs(mapped);
        verify(artistMapper).updateEntity(artist, request);
    }

    @Test
    @DisplayName("updateArtist succeeds for admin on other users artist")
    void updateArtistAdminOnOtherArtist() {
        ArtistUpdateRequest request = updateRequest();
        Artist artist = artist(OTHER_USER_ID);
        ArtistResponse mapped = new ArtistResponse();
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(artistRepository.save(artist)).thenReturn(artist);
        when(artistMapper.toResponse(artist)).thenReturn(mapped);

        ArtistResponse actual = service.updateArtist(ARTIST_ID, request);

        assertThat(actual).isSameAs(mapped);
    }

    @Test
    @DisplayName("updateArtist throws not found when artist missing")
    void updateArtistNotFound() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateArtist(ARTIST_ID, updateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    @DisplayName("updateArtist throws not found for non-owner non-admin")
    void updateArtistNonOwner() {
        Artist artist = artist(OTHER_USER_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> service.updateArtist(ARTIST_ID, updateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    @DisplayName("getArtist returns mapped response")
    void getArtistSuccess() {
        Artist artist = artist(USER_ID);
        ArtistResponse mapped = new ArtistResponse();
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(mapped);

        ArtistResponse actual = service.getArtist(ARTIST_ID);

        assertThat(actual).isSameAs(mapped);
    }

    @Test
    @DisplayName("getArtist throws not found when artist missing")
    void getArtistNotFound() {
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getArtist(ARTIST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    @DisplayName("verifyArtist updates verified flag for admin")
    void verifyArtistSuccess() {
        Artist artist = artist(USER_ID);
        ArtistVerifyRequest request = new ArtistVerifyRequest();
        request.setVerified(Boolean.TRUE);
        ArtistResponse mapped = new ArtistResponse();
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(artistRepository.save(artist)).thenReturn(artist);
        when(artistMapper.toResponse(artist)).thenReturn(mapped);

        ArtistResponse actual = service.verifyArtist(ARTIST_ID, request);

        assertThat(actual).isSameAs(mapped);
        assertThat(artist.getVerified()).isTrue();
        verify(accessValidator).requireAdmin(ROLE_ADMIN);
    }

    @Test
    @DisplayName("verifyArtist throws not found when artist missing")
    void verifyArtistNotFound() {
        ArtistVerifyRequest request = new ArtistVerifyRequest();
        request.setVerified(Boolean.TRUE);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyArtist(ARTIST_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    @DisplayName("getSummary returns counts from repositories")
    void getSummarySuccess() {
        Artist artist = artist(USER_ID);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(songRepository.countByArtistId(ARTIST_ID)).thenReturn(7L);
        when(albumRepository.countByArtistIdAndIsActiveTrue(ARTIST_ID)).thenReturn(3L);
        when(podcastRepository.countByArtistIdAndIsActiveTrue(ARTIST_ID)).thenReturn(2L);

        ArtistSummaryResponse response = service.getSummary(ARTIST_ID);

        assertThat(response.getArtistId()).isEqualTo(ARTIST_ID);
        assertThat(response.getSongCount()).isEqualTo(7L);
        assertThat(response.getAlbumCount()).isEqualTo(3L);
        assertThat(response.getPodcastCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getSummary throws not found when artist missing")
    void getSummaryNotFound() {
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(ARTIST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND);
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

    private Artist artist(Long userId) {
        Artist artist = new Artist();
        artist.setArtistId(ARTIST_ID);
        artist.setUserId(userId);
        artist.setArtistType(ArtistType.MUSIC);
        artist.setVerified(Boolean.FALSE);
        return artist;
    }
}
