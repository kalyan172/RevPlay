package com.revplay.musicplatform.playlist.repository;

import com.revplay.musicplatform.playlist.entity.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    Page<Playlist> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    Page<Playlist> findByIsPublicTrueAndIsActiveTrue(Pageable pageable);

    long countByUserId(Long userId);


    @Query("SELECT p FROM Playlist p WHERE p.isActive = true AND (p.isPublic = true OR p.userId = :userId) ORDER BY p.createdAt DESC")
    Page<Playlist> findAccessiblePlaylists(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT p
            FROM Playlist p
            WHERE p.isActive = true
              AND p.isPublic = true
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY p.createdAt DESC
            """)
    Page<Playlist> searchPublicPlaylistsByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
