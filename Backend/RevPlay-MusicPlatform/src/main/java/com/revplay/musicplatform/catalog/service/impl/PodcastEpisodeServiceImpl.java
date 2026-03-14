package com.revplay.musicplatform.catalog.service.impl;

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
import com.revplay.musicplatform.catalog.service.PodcastEpisodeService;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.AudioMetadataService;
import com.revplay.musicplatform.catalog.util.FileStorageService;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.user.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PodcastEpisodeServiceImpl implements PodcastEpisodeService {
    private static final Logger log = LoggerFactory.getLogger(PodcastEpisodeServiceImpl.class);
    private final PodcastEpisodeRepository episodeRepository;
    private final PodcastRepository podcastRepository;
    private final PodcastEpisodeMapper mapper;
    private final FileStorageService fileStorageService;
    private final SecurityUtil securityUtil;
    private final AccessValidator accessValidator;
    private final ContentValidationService contentValidationService;
    private final AudioMetadataService audioMetadataService;
    private final ArtistRepository artistRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PodcastEpisodeServiceImpl(PodcastEpisodeRepository episodeRepository, PodcastRepository podcastRepository,
                                     PodcastEpisodeMapper mapper, FileStorageService fileStorageService,
                                     SecurityUtil securityUtil, AccessValidator accessValidator,
                                     ContentValidationService contentValidationService,
                                     AudioMetadataService audioMetadataService, ArtistRepository artistRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.episodeRepository = episodeRepository;
        this.podcastRepository = podcastRepository;
        this.mapper = mapper;
        this.fileStorageService = fileStorageService;
        this.securityUtil = securityUtil;
        this.accessValidator = accessValidator;
        this.contentValidationService = contentValidationService;
        this.audioMetadataService = audioMetadataService;
        this.artistRepository = artistRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PodcastEpisodeResponse create(Long podcastId, PodcastEpisodeCreateRequest request, MultipartFile audioFile) {
        log.info("Creating episode for podcastId={}", podcastId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Podcast podcast = getOwnedPodcast(podcastId);
        Integer durationSeconds = audioMetadataService.resolveDurationSeconds(audioFile, request.getDurationSeconds());
        contentValidationService.validatePodcastEpisodeDuration(durationSeconds);
        String fileName = fileStorageService.storePodcast(audioFile);
        String audioUrl = "/api/v1/files/podcasts/" + fileName;
        PodcastEpisode episode = mapper.toEntity(request, podcast.getPodcastId(), audioUrl);
        episode.setDurationSeconds(durationSeconds);
        return mapper.toResponse(episodeRepository.save(episode));
    }

    @Override
    @Transactional
    public PodcastEpisodeResponse update(Long podcastId, Long episodeId, PodcastEpisodeUpdateRequest request) {
        log.info("Updating episodeId={}", episodeId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        PodcastEpisode episode = getOwnedEpisode(podcastId, episodeId);
        contentValidationService.validatePodcastEpisodeDuration(request.getDurationSeconds());
        mapper.updateEntity(episode, request);
        return mapper.toResponse(episodeRepository.save(episode));
    }

    @Override
    public PodcastEpisodeResponse get(Long podcastId, Long episodeId) {
        log.info("Fetching episodeId={}", episodeId);
        PodcastEpisode episode = getEpisodeByPodcast(podcastId, episodeId);
        return mapper.toResponse(episode);
    }

    @Override
    @Transactional
    public void delete(Long podcastId, Long episodeId) {
        log.info("Deleting episodeId={}", episodeId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        PodcastEpisode episode = getOwnedEpisode(podcastId, episodeId);
        String previousFile = extractFileName(episode.getAudioUrl());
        episodeRepository.delete(episode);
        if (previousFile != null) {
            fileStorageService.deletePodcastFile(previousFile);
        }
        eventPublisher.publishEvent(new PodcastEpisodeDeletedEvent(episode.getEpisodeId()));
    }

    @Override
    public Page<PodcastEpisodeResponse> listByPodcast(Long podcastId, Pageable pageable) {
        log.info("Listing episodes for podcastId={} page={}", podcastId, pageable.getPageNumber());
        return episodeRepository.findByPodcastId(podcastId, pageable)
            .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public PodcastEpisodeResponse replaceAudio(Long podcastId, Long episodeId, MultipartFile audioFile) {
        log.info("Replacing audio for episodeId={}", episodeId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        PodcastEpisode episode = getOwnedEpisode(podcastId, episodeId);
        String previousFile = extractFileName(episode.getAudioUrl());
        Integer durationSeconds = audioMetadataService.resolveDurationSeconds(audioFile, episode.getDurationSeconds());
        contentValidationService.validatePodcastEpisodeDuration(durationSeconds);
        String fileName = fileStorageService.storePodcast(audioFile);
        episode.setAudioUrl("/api/v1/files/podcasts/" + fileName);
        episode.setDurationSeconds(durationSeconds);
        PodcastEpisode saved = episodeRepository.save(episode);
        if (previousFile != null) {
            fileStorageService.deletePodcastFile(previousFile);
        }
        return mapper.toResponse(saved);
    }

    private PodcastEpisode getOwnedEpisode(Long podcastId, Long episodeId) {
        PodcastEpisode episode = getEpisodeByPodcast(podcastId, episodeId);
        getOwnedPodcast(podcastId);
        return episode;
    }

    private PodcastEpisode getEpisodeByPodcast(Long podcastId, Long episodeId) {
        PodcastEpisode episode = episodeRepository.findById(episodeId)
            .orElseThrow(() -> new ResourceNotFoundException("Episode not found"));
        if (!episode.getPodcastId().equals(podcastId)) {
            throw new ResourceNotFoundException("Episode not found");
        }
        return episode;
    }

    private Podcast getOwnedPodcast(Long podcastId) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new ResourceNotFoundException("Podcast not found"));
        String role = securityUtil.getUserRole();
        if (!UserRole.ADMIN.name().equalsIgnoreCase(role)) {
            artistRepository.findById(podcast.getArtistId())
                .filter(a -> a.getUserId().equals(securityUtil.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Podcast not found"));
        }
        return podcast;
    }

    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        int idx = fileUrl.lastIndexOf('/');
        if (idx < 0 || idx == fileUrl.length() - 1) {
            return null;
        }
        return fileUrl.substring(idx + 1);
    }
}

