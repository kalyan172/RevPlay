package com.revplay.musicplatform.catalog.service.impl;

import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.enums.ArtistType;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.audit.event.SongDeletedEvent;
import com.revplay.musicplatform.catalog.dto.request.SongCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongUpdateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongVisibilityRequest;
import com.revplay.musicplatform.catalog.dto.response.SongResponse;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.mapper.SongMapper;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.service.ContentValidationService;
import com.revplay.musicplatform.catalog.service.SongService;
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
public class SongServiceImpl implements SongService {
    private static final Logger log = LoggerFactory.getLogger(SongServiceImpl.class);
    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final SongMapper mapper;
    private final FileStorageService fileStorageService;
    private final SecurityUtil securityUtil;
    private final AccessValidator accessValidator;
    private final ContentValidationService contentValidationService;
    private final AudioMetadataService audioMetadataService;
    private final ApplicationEventPublisher eventPublisher;

    public SongServiceImpl(SongRepository songRepository, ArtistRepository artistRepository, SongMapper mapper,
                           FileStorageService fileStorageService, SecurityUtil securityUtil,
                           AccessValidator accessValidator, ContentValidationService contentValidationService,
                           AudioMetadataService audioMetadataService, ApplicationEventPublisher eventPublisher) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
        this.mapper = mapper;
        this.fileStorageService = fileStorageService;
        this.securityUtil = securityUtil;
        this.accessValidator = accessValidator;
        this.contentValidationService = contentValidationService;
        this.audioMetadataService = audioMetadataService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SongResponse create(SongCreateRequest request, MultipartFile audioFile) {
        Long currentUserId = securityUtil.getUserId();
        log.info("Creating song for currentUserId={}", currentUserId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Artist artist = getOwnedArtistByUserId();
        if (artist.getArtistType() == ArtistType.PODCAST) {
            throw new ResourceNotFoundException("Artist not allowed to upload songs");
        }
        Integer durationSeconds = audioMetadataService.resolveDurationSeconds(audioFile, request.getDurationSeconds());
        contentValidationService.validateSongDuration(durationSeconds);
        contentValidationService.validateAlbumBelongsToArtist(request.getAlbumId(), artist.getArtistId());
        contentValidationService.validateUniqueSongTitleWithinAlbum(request.getAlbumId(), request.getTitle());
        String fileName = fileStorageService.storeSong(audioFile);
        String fileUrl = "/api/v1/files/songs/" + fileName;
        Song song = mapper.toEntity(request, artist.getArtistId(), fileUrl);
        song.setDurationSeconds(durationSeconds);
        return mapper.toResponse(songRepository.save(song));
    }

    @Override
    @Transactional
    public SongResponse update(Long songId, SongUpdateRequest request) {
        log.info("Updating songId={}", songId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Song song = getOwnedSong(songId);
        contentValidationService.validateSongDuration(request.getDurationSeconds());
        contentValidationService.validateAlbumBelongsToArtist(request.getAlbumId(), song.getArtistId());
        contentValidationService.validateUniqueSongTitleWithinAlbumForUpdate(request.getAlbumId(), request.getTitle(), songId);
        mapper.updateEntity(song, request);
        return mapper.toResponse(songRepository.save(song));
    }

    @Override
    public SongResponse get(Long songId) {
        log.info("Fetching songId={}", songId);
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new ResourceNotFoundException("Song not found"));
        return mapper.toResponse(song);
    }

    @Override
    @Transactional
    public void delete(Long songId) {
        log.info("Soft deleting songId={}", songId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Song song = getOwnedSong(songId);
        song.setIsActive(Boolean.FALSE);
        songRepository.save(song);
        eventPublisher.publishEvent(new SongDeletedEvent(song.getSongId()));
    }

    @Override
    public Page<SongResponse> listByArtist(Long artistId, Pageable pageable) {
        log.info("Listing songs for artistId={} page={}", artistId, pageable.getPageNumber());
        return songRepository.findByArtistIdAndIsActiveTrue(artistId, pageable)
            .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public SongResponse updateVisibility(Long songId, SongVisibilityRequest request) {
        log.info("Updating song visibility for songId={}", songId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Song song = getOwnedSong(songId);
        if (request.getIsActive() != null) {
            song.setIsActive(request.getIsActive());
        }
        song.setVisibility(request.getVisibility());
        return mapper.toResponse(songRepository.save(song));
    }

    @Override
    @Transactional
    public SongResponse replaceAudio(Long songId, MultipartFile audioFile) {
        log.info("Replacing audio for songId={}", songId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Song song = getOwnedSong(songId);
        String previousFile = extractFileName(song.getFileUrl());
        Integer durationSeconds = audioMetadataService.resolveDurationSeconds(audioFile, song.getDurationSeconds());
        contentValidationService.validateSongDuration(durationSeconds);
        String fileName = fileStorageService.storeSong(audioFile);
        song.setFileUrl("/api/v1/files/songs/" + fileName);
        song.setDurationSeconds(durationSeconds);
        Song saved = songRepository.save(song);
        if (previousFile != null) {
            fileStorageService.deleteSongFile(previousFile);
        }
        return mapper.toResponse(saved);
    }

    private Song getOwnedSong(Long songId) {
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new ResourceNotFoundException("Song not found"));
        getOwnedArtist(song.getArtistId());
        return song;
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

