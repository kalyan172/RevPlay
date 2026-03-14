package com.revplay.musicplatform.playlist.repository;

import com.revplay.musicplatform.playlist.entity.UserLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserLikeRepository extends JpaRepository<UserLike, Long> {

    Page<UserLike> findByUserId(Long userId, Pageable pageable);

    Page<UserLike> findByUserIdAndLikeableType(Long userId, String likeableType, Pageable pageable);

    boolean existsByUserIdAndLikeableIdAndLikeableType(Long userId, Long likeableId, String likeableType);

    Optional<UserLike> findByUserIdAndLikeableIdAndLikeableType(
            Long userId, Long likeableId, String likeableType);
}
