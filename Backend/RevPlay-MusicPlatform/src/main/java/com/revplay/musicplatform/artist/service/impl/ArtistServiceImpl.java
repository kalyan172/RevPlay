package com.revplay.musicplatform.artist.service.impl;

import com.revplay.musicplatform.exception.ConflictException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.user.enums.UserRole;

import com.revplay.musicplatform.artist.dto.request.ArtistCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistUpdateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistVerifyRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistResponse;
import com.revplay.musicplatform.artist.dto.response.ArtistSummaryResponse;
import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.mapper.ArtistMapper;
import com.revplay.musicplatform.catalog.repository.AlbumRepository;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.catalog.repository.PodcastRepository;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.artist.service.ArtistService;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistServiceImpl implements ArtistService {
    private static final Logger log = LoggerFactory.getLogger(ArtistServiceImpl.class);
    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;
    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;
    private final PodcastRepository podcastRepository;
    private final SecurityUtil securityUtil;
    private final AccessValidator accessValidator;

    public ArtistServiceImpl(ArtistRepository artistRepository, ArtistMapper artistMapper,
                             SongRepository songRepository, AlbumRepository albumRepository,
                             PodcastRepository podcastRepository, SecurityUtil securityUtil,
                             AccessValidator accessValidator) {
        this.artistRepository = artistRepository;
        this.artistMapper = artistMapper;
        this.songRepository = songRepository;
        this.albumRepository = albumRepository;
        this.podcastRepository = podcastRepository;
        this.securityUtil = securityUtil;
        this.accessValidator = accessValidator;
    }

    @Override
    @Transactional
    public ArtistResponse createArtist(ArtistCreateRequest request) {
        log.info("Creating artist profile for userId={}", securityUtil.getUserId());
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Long userId = securityUtil.getUserId();
        if (artistRepository.findByUserId(userId).isPresent()) {
            throw new ConflictException("Artist profile already exists");
        }
        Artist artist = artistMapper.toEntity(request, userId);
        return artistMapper.toResponse(artistRepository.save(artist));
    }

    @Override
    @Transactional
    public ArtistResponse updateArtist(Long artistId, ArtistUpdateRequest request) {
        log.info("Updating artistId={}", artistId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Artist artist = getOwnedArtist(artistId);
        artistMapper.updateEntity(artist, request);
        return artistMapper.toResponse(artistRepository.save(artist));
    }

    @Override
    public ArtistResponse getArtist(Long artistId) {
        log.info("Fetching artistId={}", artistId);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        return artistMapper.toResponse(artist);
    }

    @Override
    @Transactional
    public ArtistResponse verifyArtist(Long artistId, ArtistVerifyRequest request) {
        log.info("Verifying artistId={} verified={}", artistId, request.getVerified());
        accessValidator.requireAdmin(securityUtil.getUserRole());
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        artist.setVerified(request.getVerified());
        return artistMapper.toResponse(artistRepository.save(artist));
    }

    @Override
    public ArtistSummaryResponse getSummary(Long artistId) {
        log.info("Fetching summary for artistId={}", artistId);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        ArtistSummaryResponse response = new ArtistSummaryResponse();
        response.setArtistId(artist.getArtistId());
        response.setSongCount(songRepository.countByArtistId(artistId));
        response.setAlbumCount(albumRepository.countByArtistIdAndIsActiveTrue(artistId));
        response.setPodcastCount(podcastRepository.countByArtistIdAndIsActiveTrue(artistId));
        return response;
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
}

