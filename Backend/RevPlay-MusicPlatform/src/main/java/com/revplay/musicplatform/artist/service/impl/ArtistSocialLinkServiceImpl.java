package com.revplay.musicplatform.artist.service.impl;


import com.revplay.musicplatform.exception.ConflictException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.user.enums.UserRole;

import java.util.List;

import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkUpdateRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistSocialLinkResponse;
import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.entity.ArtistSocialLink;
import com.revplay.musicplatform.artist.mapper.ArtistSocialLinkMapper;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.artist.repository.ArtistSocialLinkRepository;
import com.revplay.musicplatform.artist.service.ArtistSocialLinkService;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistSocialLinkServiceImpl implements ArtistSocialLinkService {
    private static final Logger log = LoggerFactory.getLogger(ArtistSocialLinkServiceImpl.class);
    private final ArtistSocialLinkRepository repository;
    private final ArtistRepository artistRepository;
    private final ArtistSocialLinkMapper mapper;
    private final SecurityUtil securityUtil;
    private final AccessValidator accessValidator;

    public ArtistSocialLinkServiceImpl(ArtistSocialLinkRepository repository, ArtistRepository artistRepository,
                                       ArtistSocialLinkMapper mapper, SecurityUtil securityUtil,
                                       AccessValidator accessValidator) {
        this.repository = repository;
        this.artistRepository = artistRepository;
        this.mapper = mapper;
        this.securityUtil = securityUtil;
        this.accessValidator = accessValidator;
    }

    @Override
    @Transactional
    public ArtistSocialLinkResponse create(Long artistId, ArtistSocialLinkCreateRequest request) {
        log.info("Creating social link for artistId={}", artistId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        ensureOwnership(artistId);
        validateUniquePlatform(artistId, request.getPlatform(), null);
        ArtistSocialLink link = mapper.toEntity(request, artistId);
        return mapper.toResponse(repository.save(link));
    }

    @Override
    public List<ArtistSocialLinkResponse> list(Long artistId) {
        log.info("Listing social links for artistId={}", artistId);
        return repository.findByArtistId(artistId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ArtistSocialLinkResponse update(Long artistId, Long linkId, ArtistSocialLinkUpdateRequest request) {
        log.info("Updating social linkId={} for artistId={}", linkId, artistId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        ensureOwnership(artistId);
        ArtistSocialLink link = repository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Social link not found"));
        if (!link.getArtistId().equals(artistId)) {
            throw new ResourceNotFoundException("Social link not found");
        }
        validateUniquePlatform(artistId, request.getPlatform(), linkId);
        mapper.updateEntity(link, request);
        return mapper.toResponse(repository.save(link));
    }

    @Override
    @Transactional
    public void delete(Long artistId, Long linkId) {
        log.info("Deleting social linkId={} for artistId={}", linkId, artistId);
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        ensureOwnership(artistId);
        ArtistSocialLink link = repository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Social link not found"));
        if (!link.getArtistId().equals(artistId)) {
            throw new ResourceNotFoundException("Social link not found");
        }
        repository.delete(link);
    }

    private void ensureOwnership(Long artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        String role = securityUtil.getUserRole();
        if (!UserRole.ADMIN.name().equalsIgnoreCase(role) && !artist.getUserId().equals(securityUtil.getUserId())) {
            throw new ResourceNotFoundException("Artist not found");
        }
    }

    private void validateUniquePlatform(Long artistId, com.revplay.musicplatform.catalog.enums.SocialPlatform platform, Long linkId) {
        boolean exists = linkId == null
                ? repository.existsByArtistIdAndPlatform(artistId, platform)
                : repository.existsByArtistIdAndPlatformAndLinkIdNot(artistId, platform, linkId);
        if (exists) {
            throw new ConflictException("Social platform already exists for this artist");
        }
    }
}

