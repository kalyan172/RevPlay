package com.revplay.musicplatform.playlist.repository;


import com.revplay.musicplatform.playlist.entity.PlaylistFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PlaylistFollowRepository extends JpaRepository<PlaylistFollow, Long> {

    List<PlaylistFollow> findByPlaylistId(Long playlistId);

    List<PlaylistFollow> findByFollowerUserId(Long followerUserId);

    boolean existsByPlaylistIdAndFollowerUserId(Long playlistId, Long followerUserId);

    Optional<PlaylistFollow> findByPlaylistIdAndFollowerUserId(Long playlistId, Long followerUserId);

    long countByPlaylistId(Long playlistId);
}