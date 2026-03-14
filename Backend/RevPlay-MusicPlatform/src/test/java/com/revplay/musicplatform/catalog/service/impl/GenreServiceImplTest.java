package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.catalog.dto.request.GenreUpsertRequest;
import com.revplay.musicplatform.catalog.dto.response.GenreResponse;
import com.revplay.musicplatform.catalog.entity.Genre;
import com.revplay.musicplatform.catalog.exception.DiscoveryNotFoundException;
import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import com.revplay.musicplatform.catalog.repository.GenreRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class GenreServiceImplTest {

    private static final Long GENRE_ID = 11L;
    private static final String NAME = "Pop";

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreServiceImpl service;

    @Test
    @DisplayName("create new genre returns created response")
    void createNewGenre() {
        GenreUpsertRequest request = new GenreUpsertRequest(NAME, "desc");
        Genre genre = genre(GENRE_ID, NAME, true);
        when(genreRepository.existsByNameIgnoreCaseAndIsActiveTrue(NAME)).thenReturn(false);
        when(genreRepository.findByNameIgnoreCaseAndIsActiveFalse(NAME)).thenReturn(Optional.empty());
        when(genreRepository.save(org.mockito.ArgumentMatchers.any(Genre.class))).thenReturn(genre);

        GenreResponse response = service.create(request);

        assertThat(response.genreId()).isEqualTo(GENRE_ID);
        assertThat(response.name()).isEqualTo(NAME);
    }

    @Test
    @DisplayName("create existing active genre name throws validation exception")
    void createExistingName() {
        GenreUpsertRequest request = new GenreUpsertRequest(NAME, "desc");
        when(genreRepository.existsByNameIgnoreCaseAndIsActiveTrue(NAME)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DiscoveryValidationException.class)
                .hasMessage("Genre name already exists");
    }

    @Test
    @DisplayName("create reactivates inactive genre with normalized description")
    void createReactivatesInactiveGenre() {
        GenreUpsertRequest request = new GenreUpsertRequest("  " + NAME + "  ", "  revived  ");
        Genre inactive = genre(GENRE_ID, NAME, false);
        inactive.setDescription("old");
        when(genreRepository.existsByNameIgnoreCaseAndIsActiveTrue(NAME)).thenReturn(false);
        when(genreRepository.findByNameIgnoreCaseAndIsActiveFalse(NAME)).thenReturn(Optional.of(inactive));
        when(genreRepository.save(inactive)).thenReturn(inactive);

        GenreResponse response = service.create(request);

        assertThat(response.genreId()).isEqualTo(GENRE_ID);
        assertThat(inactive.getIsActive()).isTrue();
        assertThat(inactive.getDescription()).isEqualTo("revived");
    }

    @Test
    @DisplayName("getAll returns active genres list")
    void getAllGenres() {
        when(genreRepository.findByIsActiveTrueOrderByNameAscGenreIdAsc()).thenReturn(List.of(genre(GENRE_ID, NAME, true)));

        List<GenreResponse> responses = service.getAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).genreId()).isEqualTo(GENRE_ID);
    }

    @Test
    @DisplayName("getById existing genre returns response")
    void getById() {
        when(genreRepository.findByGenreIdAndIsActiveTrue(GENRE_ID)).thenReturn(Optional.of(genre(GENRE_ID, NAME, true)));

        GenreResponse response = service.getById(GENRE_ID);

        assertThat(response.name()).isEqualTo(NAME);
    }

    @Test
    @DisplayName("getById missing genre throws not found")
    void getByIdNotFound() {
        when(genreRepository.findByGenreIdAndIsActiveTrue(GENRE_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(GENRE_ID))
                .isInstanceOf(DiscoveryNotFoundException.class)
                .hasMessage("Genre " + GENRE_ID + " not found");
    }

    @Test
    @DisplayName("update existing genre saves normalized values")
    void updateGenre() {
        Genre genre = genre(GENRE_ID, NAME, true);
        when(genreRepository.findByGenreIdAndIsActiveTrue(GENRE_ID)).thenReturn(Optional.of(genre));
        when(genreRepository.existsByNameIgnoreCaseAndIsActiveTrueAndGenreIdNot("Indie", GENRE_ID)).thenReturn(false);
        when(genreRepository.save(genre)).thenReturn(genre);

        GenreResponse response = service.update(GENRE_ID, new GenreUpsertRequest("  Indie  ", "  airy  "));

        assertThat(response.genreId()).isEqualTo(GENRE_ID);
        assertThat(genre.getName()).isEqualTo("Indie");
        assertThat(genre.getDescription()).isEqualTo("airy");
    }

    @Test
    @DisplayName("update duplicate active name throws validation exception")
    void updateDuplicateName() {
        when(genreRepository.findByGenreIdAndIsActiveTrue(GENRE_ID)).thenReturn(Optional.of(genre(GENRE_ID, NAME, true)));
        when(genreRepository.existsByNameIgnoreCaseAndIsActiveTrueAndGenreIdNot("Indie", GENRE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(GENRE_ID, new GenreUpsertRequest("Indie", "desc")))
                .isInstanceOf(DiscoveryValidationException.class)
                .hasMessage("Genre name already exists");
    }

    @Test
    @DisplayName("update with blank description stores null")
    void updateBlankDescriptionStoresNull() {
        Genre genre = genre(GENRE_ID, NAME, true);
        genre.setDescription("before");
        when(genreRepository.findByGenreIdAndIsActiveTrue(GENRE_ID)).thenReturn(Optional.of(genre));
        when(genreRepository.existsByNameIgnoreCaseAndIsActiveTrueAndGenreIdNot(NAME, GENRE_ID)).thenReturn(false);
        when(genreRepository.save(genre)).thenReturn(genre);

        service.update(GENRE_ID, new GenreUpsertRequest(NAME, "   "));

        assertThat(genre.getDescription()).isNull();
    }

    @Test
    @DisplayName("delete soft deletes active genre")
    void deleteGenre() {
        Genre genre = genre(GENRE_ID, NAME, true);
        when(genreRepository.findByGenreIdAndIsActiveTrue(GENRE_ID)).thenReturn(Optional.of(genre));
        when(genreRepository.save(genre)).thenReturn(genre);

        service.delete(GENRE_ID);

        assertThat(genre.getIsActive()).isFalse();
        verify(genreRepository).save(genre);
    }

    @Test
    @DisplayName("create rejects too long description")
    void createRejectsTooLongDescription() {
        String longDescription = "a".repeat(1001);

        assertThatThrownBy(() -> service.create(new GenreUpsertRequest(NAME, longDescription)))
                .isInstanceOf(DiscoveryValidationException.class)
                .hasMessage("description must be at most 1000 characters");
    }

    private Genre genre(Long id, String name, boolean active) {
        Genre genre = new Genre();
        genre.setGenreId(id);
        genre.setName(name);
        genre.setIsActive(active);
        return genre;
    }
}
