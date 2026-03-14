package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.enums.ArtistType;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.audit.event.PodcastDeletedEvent;
import com.revplay.musicplatform.catalog.dto.request.PodcastCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.PodcastUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastResponse;
import com.revplay.musicplatform.catalog.entity.Podcast;
import com.revplay.musicplatform.catalog.mapper.PodcastMapper;
import com.revplay.musicplatform.catalog.repository.PodcastRepository;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PodcastServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long ARTIST_ID = 20L;
    private static final Long PODCAST_ID = 30L;
    private static final String ROLE_ARTIST = "ARTIST";

    @Mock
    private PodcastRepository podcastRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private PodcastMapper mapper;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AccessValidator accessValidator;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Captor
    private ArgumentCaptor<PodcastDeletedEvent> eventCaptor;

    @InjectMocks
    private PodcastServiceImpl service;

    @Test
    @DisplayName("podcast-type artist can create podcast")
    void createPodcastByPodcastArtist() {
        PodcastCreateRequest request = new PodcastCreateRequest();
        request.setTitle("Talk");
        Podcast podcast = podcast(PODCAST_ID, ARTIST_ID, true);
        PodcastResponse response = podcastResponse(PODCAST_ID, "Talk");
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.PODCAST)));
        when(mapper.toEntity(request, ARTIST_ID)).thenReturn(podcast);
        when(podcastRepository.save(podcast)).thenReturn(podcast);
        when(mapper.toResponse(podcast)).thenReturn(response);

        PodcastResponse actual = service.create(request);

        assertThat(actual.getPodcastId()).isEqualTo(PODCAST_ID);
    }

    @Test
    @DisplayName("music-type artist cannot create podcast")
    void musicArtistCannotCreatePodcast() {
        PodcastCreateRequest request = new PodcastCreateRequest();
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Artist not allowed to create podcasts");
    }

    @Test
    @DisplayName("update owned podcast returns updated response")
    void updateOwnedPodcast() {
        Podcast podcast = podcast(PODCAST_ID, ARTIST_ID, true);
        PodcastUpdateRequest request = new PodcastUpdateRequest();
        request.setTitle("Updated");
        PodcastResponse response = podcastResponse(PODCAST_ID, "Updated");
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.PODCAST)));
        when(podcastRepository.save(podcast)).thenReturn(podcast);
        when(mapper.toResponse(podcast)).thenReturn(response);

        PodcastResponse actual = service.update(PODCAST_ID, request);

        verify(mapper).updateEntity(podcast, request);
        assertThat(actual.getTitle()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("delete owned podcast soft deletes and publishes event")
    void deleteOwnedPodcast() {
        Podcast podcast = podcast(PODCAST_ID, ARTIST_ID, true);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.PODCAST)));
        when(podcastRepository.save(podcast)).thenReturn(podcast);

        service.delete(PODCAST_ID);

        assertThat(podcast.getIsActive()).isFalse();
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPodcastId()).isEqualTo(PODCAST_ID);
    }

    @Test
    @DisplayName("get missing podcast throws not found")
    void getPodcastNotFound() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(PODCAST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Podcast not found");
    }

    private Podcast podcast(Long id, Long artistId, boolean active) {
        Podcast podcast = new Podcast();
        podcast.setPodcastId(id);
        podcast.setArtistId(artistId);
        podcast.setIsActive(active);
        podcast.setTitle("Talk");
        return podcast;
    }

    private PodcastResponse podcastResponse(Long id, String title) {
        PodcastResponse response = new PodcastResponse();
        response.setPodcastId(id);
        response.setTitle(title);
        return response;
    }

    private Artist artist(Long artistId, Long userId, ArtistType type) {
        Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setUserId(userId);
        artist.setArtistType(type);
        return artist;
    }
}
