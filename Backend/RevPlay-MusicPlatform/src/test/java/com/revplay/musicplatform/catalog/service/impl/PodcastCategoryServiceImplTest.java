package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.catalog.dto.request.PodcastCategoryCreateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastCategoryResponse;
import com.revplay.musicplatform.catalog.entity.PodcastCategory;
import com.revplay.musicplatform.catalog.mapper.PodcastCategoryMapper;
import com.revplay.musicplatform.catalog.repository.PodcastCategoryRepository;
import com.revplay.musicplatform.exception.ConflictException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PodcastCategoryServiceImplTest {

    private static final String CATEGORY_NAME = "Tech";
    private static final String CATEGORY_DESC = "Technology";
    private static final String DUPLICATE_MESSAGE = "Category already exists";

    @Mock
    private PodcastCategoryRepository repository;
    @Mock
    private PodcastCategoryMapper mapper;

    @InjectMocks
    private PodcastCategoryServiceImpl service;

    @Test
    @DisplayName("create saves new category when name is unique")
    void createSuccess() {
        PodcastCategoryCreateRequest request = request(CATEGORY_NAME, CATEGORY_DESC);
        PodcastCategory entity = new PodcastCategory();
        PodcastCategory saved = new PodcastCategory();
        PodcastCategoryResponse response = new PodcastCategoryResponse();

        when(repository.existsByNameIgnoreCase(CATEGORY_NAME)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        PodcastCategoryResponse actual = service.create(request);

        assertThat(actual).isSameAs(response);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("create throws conflict when category name exists")
    void createConflict() {
        PodcastCategoryCreateRequest request = request(CATEGORY_NAME, CATEGORY_DESC);
        when(repository.existsByNameIgnoreCase(CATEGORY_NAME)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage(DUPLICATE_MESSAGE);
    }

    @Test
    @DisplayName("list returns mapped categories")
    void listReturnsMappedCategories() {
        PodcastCategory first = new PodcastCategory();
        PodcastCategory second = new PodcastCategory();
        PodcastCategoryResponse firstResponse = new PodcastCategoryResponse();
        PodcastCategoryResponse secondResponse = new PodcastCategoryResponse();

        when(repository.findAll()).thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        List<PodcastCategoryResponse> actual = service.list();

        assertThat(actual).containsExactly(firstResponse, secondResponse);
    }

    private PodcastCategoryCreateRequest request(String name, String description) {
        PodcastCategoryCreateRequest request = new PodcastCategoryCreateRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }
}
