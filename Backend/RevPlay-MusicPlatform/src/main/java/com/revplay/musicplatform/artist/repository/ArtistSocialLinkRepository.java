package com.revplay.musicplatform.artist.repository;



import java.util.List;

import com.revplay.musicplatform.artist.entity.ArtistSocialLink;
import com.revplay.musicplatform.catalog.enums.SocialPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistSocialLinkRepository extends JpaRepository<ArtistSocialLink, Long> {
    List<ArtistSocialLink> findByArtistId(Long artistId);
    boolean existsByArtistIdAndPlatform(Long artistId, SocialPlatform platform);
    boolean existsByArtistIdAndPlatformAndLinkIdNot(Long artistId, SocialPlatform platform, Long linkId);
}

