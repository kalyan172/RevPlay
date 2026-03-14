package com.revplay.musicplatform.catalog.service.impl;

import com.revplay.musicplatform.catalog.service.GenreService;
import com.revplay.musicplatform.catalog.dto.request.GenreUpsertRequest;
import com.revplay.musicplatform.catalog.dto.response.GenreResponse;
import com.revplay.musicplatform.catalog.entity.Genre;
import com.revplay.musicplatform.catalog.exception.DiscoveryNotFoundException;
import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import com.revplay.musicplatform.catalog.repository.GenreRepository;
import com.revplay.musicplatform.catalog.util.DiscoveryValidationUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenreServiceImpl implements GenreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenreService.class);

    private final GenreRepository genreRepository;

    public GenreServiceImpl(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "genres", key = "'all'")
    public List<GenreResponse> getAll() {
        LOGGER.debug("Fetching all active genres");
        return genreRepository.findByIsActiveTrueOrderByNameAscGenreIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "genres", key = "'id:' + #genreId")
    public GenreResponse getById(Long genreId) {
        LOGGER.debug("Fetching genre by id={}", genreId);
        DiscoveryValidationUtil.requirePositiveId(genreId, "genreId");
        Genre genre = genreRepository.findByGenreIdAndIsActiveTrue(genreId)
                .orElseThrow(() -> new DiscoveryNotFoundException("Genre " + genreId + " not found"));
        return toResponse(genre);
    }

    @Transactional
    @CacheEvict(cacheNames = "genres", allEntries = true)
    public GenreResponse create(GenreUpsertRequest request) {
        LOGGER.info("Creating genre");
        String normalizedName = normalizeName(request.name());
        String normalizedDescription = normalizeDescription(request.description());

        if (genreRepository.existsByNameIgnoreCaseAndIsActiveTrue(normalizedName)) {
            LOGGER.warn("Genre name already exists for create");
            throw new DiscoveryValidationException("Genre name already exists");
        }

        Genre reactivatable = genreRepository.findByNameIgnoreCaseAndIsActiveFalse(normalizedName).orElse(null);
        if (reactivatable != null) {
            reactivatable.setIsActive(Boolean.TRUE);
            reactivatable.setDescription(normalizedDescription);
            Genre saved = genreRepository.save(reactivatable);
            LOGGER.info("Reactivated existing genre id={}", saved.getGenreId());
            return toResponse(saved);
        }

        Genre entity = new Genre();
        entity.setName(normalizedName);
        entity.setDescription(normalizedDescription);
        entity.setIsActive(Boolean.TRUE);
        Genre saved = genreRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "genres", allEntries = true)
    public GenreResponse update(Long genreId, GenreUpsertRequest request) {
        LOGGER.info("Updating genre id={}", genreId);
        DiscoveryValidationUtil.requirePositiveId(genreId, "genreId");
        String normalizedName = normalizeName(request.name());
        String normalizedDescription = normalizeDescription(request.description());

        Genre genre = genreRepository.findByGenreIdAndIsActiveTrue(genreId)
                .orElseThrow(() -> new DiscoveryNotFoundException("Genre " + genreId + " not found"));

        if (genreRepository.existsByNameIgnoreCaseAndIsActiveTrueAndGenreIdNot(normalizedName, genreId)) {
            LOGGER.warn("Genre name already exists for update, genreId={}", genreId);
            throw new DiscoveryValidationException("Genre name already exists");
        }

        genre.setName(normalizedName);
        genre.setDescription(normalizedDescription);
        Genre saved = genreRepository.save(genre);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "genres", allEntries = true)
    public void delete(Long genreId) {
        LOGGER.info("Soft deleting genre id={}", genreId);
        DiscoveryValidationUtil.requirePositiveId(genreId, "genreId");

        Genre genre = genreRepository.findByGenreIdAndIsActiveTrue(genreId)
                .orElseThrow(() -> new DiscoveryNotFoundException("Genre " + genreId + " not found"));

        genre.setIsActive(Boolean.FALSE);
        genreRepository.save(genre);
    }

    private String normalizeName(String name) {
        DiscoveryValidationUtil.requireNotBlank(name, "name");
        String normalized = name.trim();
        if (normalized.length() > 100) {
            throw new DiscoveryValidationException("name must be at most 100 characters");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 1000) {
            throw new DiscoveryValidationException("description must be at most 1000 characters");
        }
        return normalized;
    }

    private GenreResponse toResponse(Genre entity) {
        return new GenreResponse(
                entity.getGenreId(),
                entity.getName(),
                entity.getDescription(),
                entity.getIsActive()
        );
    }
}



