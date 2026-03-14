package com.revplay.musicplatform.catalog.repository;

import com.revplay.musicplatform.catalog.entity.Genre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    List<Genre> findByIsActiveTrueOrderByNameAscGenreIdAsc();

    Optional<Genre> findByGenreIdAndIsActiveTrue(Long genreId);

    boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndIsActiveTrueAndGenreIdNot(String name, Long genreId);

    Optional<Genre> findByNameIgnoreCaseAndIsActiveFalse(String name);

    long countByGenreIdIn(List<Long> genreIds);
}


