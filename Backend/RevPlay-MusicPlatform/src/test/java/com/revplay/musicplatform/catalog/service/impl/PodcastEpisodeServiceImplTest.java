package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.audit.event.PodcastEpisodeDeletedEvent;
import com.revplay.musicplatform.catalog.dto.request.PodcastEpisodeCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.PodcastEpisodeUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastEpisodeResponse;
import com.revplay.musicplatform.catalog.entity.Podcast;
import com.revplay.musicplatform.catalog.entity.PodcastEpisode;
import com.revplay.musicplatform.catalog.mapper.PodcastEpisodeMapper;
import com.revplay.musicplatform.catalog.repository.PodcastEpisodeRepository;
import com.revplay.musicplatform.catalog.repository.PodcastRepository;
import com.revplay.musicplatform.catalog.service.ContentValidationService;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.AudioMetadataService;
import com.revplay.musicplatform.catalog.util.FileStorageService;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PodcastEpisodeServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long ARTIST_ID = 20L;
    private static final Long PODCAST_ID = 30L;
    private static final Long EPISODE_ID = 40L;
    private static final String ROLE_ARTIST = "ARTIST";

    @Mock
    private PodcastEpisodeRepository episodeRepository;
    @Mock
    private PodcastRepository podcastRepository;
    @Mock
    private PodcastEpisodeMapper mapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AccessValidator accessValidator;
    @Mock
    private ContentValidationService contentValidationService;
    @Mock
    private AudioMetadataService audioMetadataService;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MultipartFile audioFile;
    @Captor
    private ArgumentCaptor<PodcastEpisodeDeletedEvent> eventCaptor;

    @InjectMocks
    private PodcastEpisodeServiceImpl service;

    @Test
    @DisplayName("create episode stores file and returns response")
    void createEpisode() {
        Podcast podcast = podcast(PODCAST_ID, ARTIST_ID);
        PodcastEpisodeCreateRequest request = new PodcastEpisodeCreateRequest();
        request.setTitle("Ep1");
        request.setDurationSeconds(1200);
        PodcastEpisode episode = episode(EPISODE_ID, PODCAST_ID, "/api/v1/files/podcasts/ep.mp3");
        PodcastEpisodeResponse response = episodeResponse(EPISODE_ID, "Ep1");
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(audioMetadataService.resolveDurationSeconds(audioFile, 1200)).thenReturn(1200);
        when(fileStorageService.storePodcast(audioFile)).thenReturn("ep.mp3");
        when(mapper.toEntity(request, PODCAST_ID, "/api/v1/files/podcasts/ep.mp3")).thenReturn(episode);
        when(episodeRepository.save(episode)).thenReturn(episode);
        when(mapper.toResponse(episode)).thenReturn(response);

        PodcastEpisodeResponse actual = service.create(PODCAST_ID, request, audioFile);

        assertThat(actual.getEpisodeId()).isEqualTo(EPISODE_ID);
    }

    @Test
    @DisplayName("update owned episode returns updated response")
    void updateOwnedEpisode() {
        PodcastEpisode episode = episode(EPISODE_ID, PODCAST_ID, "/api/v1/files/podcasts/ep.mp3");
        PodcastEpisodeUpdateRequest request = new PodcastEpisodeUpdateRequest();
        request.setTitle("Updated");
        request.setDurationSeconds(1500);
        PodcastEpisodeResponse response = episodeResponse(EPISODE_ID, "Updated");
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(episodeRepository.findById(EPISODE_ID)).thenReturn(Optional.of(episode));
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(PODCAST_ID, ARTIST_ID)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(episodeRepository.save(episode)).thenReturn(episode);
        when(mapper.toResponse(episode)).thenReturn(response);

        PodcastEpisodeResponse actual = service.update(PODCAST_ID, EPISODE_ID, request);

        verify(mapper).updateEntity(episode, request);
        assertThat(actual.getTitle()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("delete owned episode removes record and publishes event")
    void deleteOwnedEpisode() {
        PodcastEpisode episode = episode(EPISODE_ID, PODCAST_ID, "/api/v1/files/podcasts/old.mp3");
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(episodeRepository.findById(EPISODE_ID)).thenReturn(Optional.of(episode));
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(PODCAST_ID, ARTIST_ID)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));

        service.delete(PODCAST_ID, EPISODE_ID);

        verify(episodeRepository).delete(episode);
        verify(fileStorageService).deletePodcastFile("old.mp3");
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEpisodeId()).isEqualTo(EPISODE_ID);
    }

    @Test
    @DisplayName("delete with blank audio url does not call delete file")
    void deleteBlankAudioUrlNoDeleteFile() {
        PodcastEpisode episode = episode(EPISODE_ID, PODCAST_ID, " ");
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(episodeRepository.findById(EPISODE_ID)).thenReturn(Optional.of(episode));
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(PODCAST_ID, ARTIST_ID)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));

        service.delete(PODCAST_ID, EPISODE_ID);

        verify(fileStorageService, never()).deletePodcastFile(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("non-owner cannot update episode and gets not found")
    void nonOwnerCannotUpdate() {
        PodcastEpisode episode = episode(EPISODE_ID, PODCAST_ID, "/api/v1/files/podcasts/ep.mp3");
        PodcastEpisodeUpdateRequest request = new PodcastEpisodeUpdateRequest();
        request.setTitle("Updated");
        request.setDurationSeconds(1500);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(episodeRepository.findById(EPISODE_ID)).thenReturn(Optional.of(episode));
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(PODCAST_ID, ARTIST_ID)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID + 1)));

        assertThatThrownBy(() -> service.update(PODCAST_ID, EPISODE_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Podcast not found");
    }

    @Test
    @DisplayName("get throws when episode belongs to another podcast")
    void getEpisodeFromAnotherPodcast() {
        when(episodeRepository.findById(EPISODE_ID))
                .thenReturn(Optional.of(episode(EPISODE_ID, PODCAST_ID + 1, "/api/v1/files/podcasts/ep.mp3")));

        assertThatThrownBy(() -> service.get(PODCAST_ID, EPISODE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Episode not found");
    }

    @Test
    @DisplayName("replace audio deletes previous file when old file name is present")
    void replaceAudioDeletesPreviousFile() {
        PodcastEpisode episode = episode(EPISODE_ID, PODCAST_ID, "/api/v1/files/podcasts/old.mp3");
        PodcastEpisodeResponse response = episodeResponse(EPISODE_ID, "Ep1");
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(episodeRepository.findById(EPISODE_ID)).thenReturn(Optional.of(episode));
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(PODCAST_ID, ARTIST_ID)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(audioMetadataService.resolveDurationSeconds(audioFile, episode.getDurationSeconds())).thenReturn(900);
        when(fileStorageService.storePodcast(audioFile)).thenReturn("new.mp3");
        when(episodeRepository.save(episode)).thenReturn(episode);
        when(mapper.toResponse(episode)).thenReturn(response);

        PodcastEpisodeResponse actual = service.replaceAudio(PODCAST_ID, EPISODE_ID, audioFile);

        assertThat(actual.getEpisodeId()).isEqualTo(EPISODE_ID);
        assertThat(episode.getAudioUrl()).isEqualTo("/api/v1/files/podcasts/new.mp3");
        assertThat(episode.getDurationSeconds()).isEqualTo(900);
        verify(fileStorageService).deletePodcastFile("old.mp3");
    }

    @Test
    @DisplayName("replace audio skips delete when previous file name cannot be extracted")
    void replaceAudioSkipsDeleteWhenPreviousFileMissing() {
        PodcastEpisode episode = episode(EPISODE_ID, PODCAST_ID, "invalid-url");
        PodcastEpisodeResponse response = episodeResponse(EPISODE_ID, "Ep1");
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(episodeRepository.findById(EPISODE_ID)).thenReturn(Optional.of(episode));
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(PODCAST_ID, ARTIST_ID)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(audioMetadataService.resolveDurationSeconds(audioFile, episode.getDurationSeconds())).thenReturn(900);
        when(fileStorageService.storePodcast(audioFile)).thenReturn("new.mp3");
        when(episodeRepository.save(episode)).thenReturn(episode);
        when(mapper.toResponse(episode)).thenReturn(response);

        service.replaceAudio(PODCAST_ID, EPISODE_ID, audioFile);

        verify(fileStorageService, never()).deletePodcastFile(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("listByPodcast maps repository page to response page")
    void listByPodcastMapsResponses() {
        PodcastEpisode first = episode(EPISODE_ID, PODCAST_ID, "/api/v1/files/podcasts/ep.mp3");
        PodcastEpisodeResponse firstResponse = episodeResponse(EPISODE_ID, "Ep1");
        when(episodeRepository.findByPodcastId(PODCAST_ID, PageRequest.of(0, 2)))
                .thenReturn(new PageImpl<>(java.util.List.of(first), PageRequest.of(0, 2), 1));
        when(mapper.toResponse(first)).thenReturn(firstResponse);

        var page = service.listByPodcast(PODCAST_ID, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEpisodeId()).isEqualTo(EPISODE_ID);
    }

    private Podcast podcast(Long podcastId, Long artistId) {
        Podcast podcast = new Podcast();
        podcast.setPodcastId(podcastId);
        podcast.setArtistId(artistId);
        return podcast;
    }

    private PodcastEpisode episode(Long episodeId, Long podcastId, String audioUrl) {
        PodcastEpisode episode = new PodcastEpisode();
        episode.setEpisodeId(episodeId);
        episode.setPodcastId(podcastId);
        episode.setAudioUrl(audioUrl);
        return episode;
    }

    private PodcastEpisodeResponse episodeResponse(Long episodeId, String title) {
        PodcastEpisodeResponse response = new PodcastEpisodeResponse();
        response.setEpisodeId(episodeId);
        response.setTitle(title);
        return response;
    }

    private Artist artist(Long artistId, Long userId) {
        Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setUserId(userId);
        return artist;
    }
}
