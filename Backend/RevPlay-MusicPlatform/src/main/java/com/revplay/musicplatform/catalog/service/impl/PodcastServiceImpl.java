package com.revplay.musicplatform.catalog.service.impl;

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
import com.revplay.musicplatform.catalog.service.PodcastService;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.user.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PodcastServiceImpl implements PodcastService {
    private static final Logger log = LoggerFactory.getLogger(PodcastServiceImpl.class);
    private final PodcastRepository podcastRepository;
    private final ArtistRepository artistRepository;
    private final PodcastMapper mapper;
    private final SecurityUtil securityUtil;
    private final AccessValidator accessValidator;
    private final ApplicationEventPublisher eventPublisher;

    public PodcastServiceImpl(PodcastRepository podcastRepository, ArtistRepository artistRepository,
                              PodcastMapper mapper, SecurityUtil securityUtil,
                              AccessValidator accessValidator, ApplicationEventPublisher eventPublisher) {
        this.podcastRepository = podcastRepository;
        this.artistRepository = artistRepository;
        this.mapper = mapper;
        this.securityUtil = securityUtil;
        this.accessValidator = accessValidator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PodcastResponse create(PodcastCreateRequest request) {
        Long currentUserId = securityUtil.getUserId();
        log.info("Creating podcast for currentUserId={}", currentUserId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Artist artist = getOwnedArtistByUserId();
        if (artist.getArtistType() == ArtistType.MUSIC) {
            throw new BadRequestException("Artist not allowed to create podcasts");
        }
        Podcast podcast = mapper.toEntity(request, artist.getArtistId());
        return mapper.toResponse(podcastRepository.save(podcast));
    }

    @Override
    @Transactional
    public PodcastResponse update(Long podcastId, PodcastUpdateRequest request) {
        log.info("Updating podcastId={}", podcastId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Podcast podcast = getOwnedPodcast(podcastId);
        mapper.updateEntity(podcast, request);
        return mapper.toResponse(podcastRepository.save(podcast));
    }

    @Override
    public PodcastResponse get(Long podcastId) {
        log.info("Fetching podcastId={}", podcastId);
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new ResourceNotFoundException("Podcast not found"));
        if (Boolean.FALSE.equals(podcast.getIsActive())) {
            throw new ResourceNotFoundException("Podcast not found");
        }
        return mapper.toResponse(podcast);
    }

    @Override
    @Transactional
    public void delete(Long podcastId) {
        log.info("Deleting podcastId={}", podcastId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Podcast podcast = getOwnedPodcast(podcastId);
        podcast.setIsActive(Boolean.FALSE);
        podcastRepository.save(podcast);
        eventPublisher.publishEvent(new PodcastDeletedEvent(podcast.getPodcastId()));
    }

    @Override
    public Page<PodcastResponse> listByArtist(Long artistId, Pageable pageable) {
        log.info("Listing podcasts for artistId={} page={}", artistId, pageable.getPageNumber());
        return podcastRepository.findByArtistIdAndIsActiveTrue(artistId, pageable)
            .map(mapper::toResponse);
    }

    @Override
    public Page<PodcastResponse> listRecommended(Pageable pageable) {
        log.info("Listing recommended podcasts page={}", pageable.getPageNumber());
        return podcastRepository.findRecommended(pageable)
            .map(mapper::toResponse);
    }

    private Podcast getOwnedPodcast(Long podcastId) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new ResourceNotFoundException("Podcast not found"));
        if (Boolean.FALSE.equals(podcast.getIsActive())) {
            throw new ResourceNotFoundException("Podcast not found");
        }
        getOwnedArtist(podcast.getArtistId());
        return podcast;
    }

    private Artist getOwnedArtist(Long artistId) {
        Artist artist = artistRepository.findById(artistId)
            .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        String role = securityUtil.getUserRole();
        if (!UserRole.ADMIN.name().equalsIgnoreCase(role) && !artist.getUserId().equals(securityUtil.getUserId())) {
            throw new ResourceNotFoundException("Artist not found");
        }
        return artist;
    }

    private Artist getOwnedArtistByUserId() {
        return artistRepository.findByUserId(securityUtil.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Artist profile not found"));
    }
}
