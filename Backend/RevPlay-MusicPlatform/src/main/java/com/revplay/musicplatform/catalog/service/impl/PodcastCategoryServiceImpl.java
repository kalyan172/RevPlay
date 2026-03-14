package com.revplay.musicplatform.catalog.service.impl;



import java.util.List;

import com.revplay.musicplatform.catalog.dto.request.PodcastCategoryCreateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastCategoryResponse;
import com.revplay.musicplatform.catalog.entity.PodcastCategory;
import com.revplay.musicplatform.catalog.mapper.PodcastCategoryMapper;
import com.revplay.musicplatform.catalog.repository.PodcastCategoryRepository;
import com.revplay.musicplatform.catalog.service.PodcastCategoryService;
import com.revplay.musicplatform.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PodcastCategoryServiceImpl implements PodcastCategoryService {
    private static final Logger log = LoggerFactory.getLogger(PodcastCategoryServiceImpl.class);
    private final PodcastCategoryRepository repository;
    private final PodcastCategoryMapper mapper;

    public PodcastCategoryServiceImpl(PodcastCategoryRepository repository, PodcastCategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PodcastCategoryResponse create(PodcastCategoryCreateRequest request) {
        log.info("Creating podcast category name={}", request.getName());
        if (repository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("Category already exists");
        }
        PodcastCategory category = mapper.toEntity(request);
        return mapper.toResponse(repository.save(category));
    }

    @Override
    public List<PodcastCategoryResponse> list() {
        log.info("Listing podcast categories");
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }
}
