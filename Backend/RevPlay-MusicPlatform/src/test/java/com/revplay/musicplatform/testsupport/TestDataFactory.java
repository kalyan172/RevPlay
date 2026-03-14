package com.revplay.musicplatform.testsupport;

import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.enums.ArtistType;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.enums.ContentVisibility;
import com.revplay.musicplatform.playlist.entity.Playlist;
import com.revplay.musicplatform.premium.entity.UserSubscription;
import com.revplay.musicplatform.premium.enums.PlanType;
import com.revplay.musicplatform.premium.enums.SubscriptionStatus;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.enums.UserRole;
import java.time.Instant;
import java.time.LocalDateTime;

public final class TestDataFactory {

    private static final String DEFAULT_PASSWORD_HASH = "test-password-hash";
    private static final int DEFAULT_SONG_DURATION_SECONDS = 180;
    private static final String DEFAULT_SONG_FILE_URL = "/test/song.mp3";

    private TestDataFactory() {
    }

    public static User buildUser(Long id, String email, String username, UserRole role) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setPasswordHash(DEFAULT_PASSWORD_HASH);
        user.setEmailVerified(Boolean.TRUE);
        user.setIsActive(Boolean.TRUE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    public static User buildUnverifiedUser(Long id, String email, String username) {
        User user = buildUser(id, email, username, UserRole.LISTENER);
        user.setEmailVerified(Boolean.FALSE);
        return user;
    }

    public static Artist buildArtist(Long artistId, Long userId, String name, ArtistType type) {
        Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setUserId(userId);
        artist.setDisplayName(name);
        artist.setArtistType(type);
        artist.setVerified(Boolean.FALSE);
        artist.setCreatedAt(LocalDateTime.now());
        artist.setUpdatedAt(LocalDateTime.now());
        return artist;
    }

    public static Song buildSong(Long songId, Long artistId, String title) {
        Song song = new Song();
        song.setSongId(songId);
        song.setArtistId(artistId);
        song.setTitle(title);
        song.setDurationSeconds(DEFAULT_SONG_DURATION_SECONDS);
        song.setFileUrl(DEFAULT_SONG_FILE_URL);
        song.setIsActive(Boolean.TRUE);
        song.setVisibility(ContentVisibility.PUBLIC);
        song.setCreatedAt(LocalDateTime.now());
        return song;
    }

    public static Playlist buildPlaylist(Long id, Long userId, String name, boolean isPublic) {
        Playlist playlist = new Playlist();
        playlist.setId(id);
        playlist.setUserId(userId);
        playlist.setName(name);
        playlist.setIsPublic(isPublic);
        playlist.setIsActive(Boolean.TRUE);
        playlist.setCreatedAt(LocalDateTime.now());
        playlist.setUpdatedAt(LocalDateTime.now());
        return playlist;
    }

    public static UserSubscription buildSubscription(Long userId, SubscriptionStatus status, LocalDateTime endDate) {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(userId);
        subscription.setStatus(status);
        subscription.setPlanType(PlanType.MONTHLY);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(endDate);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());
        return subscription;
    }
}
