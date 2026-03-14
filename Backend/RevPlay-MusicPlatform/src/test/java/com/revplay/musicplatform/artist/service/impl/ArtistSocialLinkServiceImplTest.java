package com.revplay.musicplatform.artist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkUpdateRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistSocialLinkResponse;
import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.entity.ArtistSocialLink;
import com.revplay.musicplatform.artist.mapper.ArtistSocialLinkMapper;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.artist.repository.ArtistSocialLinkRepository;
import com.revplay.musicplatform.catalog.enums.SocialPlatform;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import com.revplay.musicplatform.exception.ConflictException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
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
class ArtistSocialLinkServiceImplTest {

    private static final Long ARTIST_ID = 100L;
    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;
    private static final Long LINK_ID = 7L;
    private static final String ROLE_ARTIST = "ARTIST";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String DUPLICATE_PLATFORM = "Social platform already exists for this artist";
    private static final String LINK_NOT_FOUND = "Social link not found";
    private static final String ARTIST_NOT_FOUND = "Artist not found";

    @Mock
    private ArtistSocialLinkRepository repository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ArtistSocialLinkMapper mapper;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AccessValidator accessValidator;

    @InjectMocks
    private ArtistSocialLinkServiceImpl service;

    @Test
    @DisplayName("create saves social link for owned artist")
    void createSuccess() {
        ArtistSocialLinkCreateRequest request = createRequest();
        Artist artist = artist(USER_ID);
        ArtistSocialLink entity = link(ARTIST_ID);
        ArtistSocialLinkResponse response = new ArtistSocialLinkResponse();
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(repository.existsByArtistIdAndPlatform(ARTIST_ID, SocialPlatform.INSTAGRAM)).thenReturn(false);
        when(mapper.toEntity(request, ARTIST_ID)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        ArtistSocialLinkResponse actual = service.create(ARTIST_ID, request);

        assertThat(actual).isSameAs(response);
        verify(accessValidator).requireArtistOrAdmin(ROLE_ARTIST);
    }

    @Test
    @DisplayName("create throws conflict for duplicate platform")
    void createDuplicatePlatform() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(USER_ID)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(repository.existsByArtistIdAndPlatform(ARTIST_ID, SocialPlatform.INSTAGRAM)).thenReturn(true);

        assertThatThrownBy(() -> service.create(ARTIST_ID, createRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage(DUPLICATE_PLATFORM);
    }

    @Test
    @DisplayName("create throws not found when artist missing")
    void createArtistMissing() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ARTIST_ID, createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ARTIST_NOT_FOUND);
    }

    @Test
    @DisplayName("list maps all social links")
    void listSuccess() {
        ArtistSocialLink existing = link(ARTIST_ID);
        ArtistSocialLinkResponse response = new ArtistSocialLinkResponse();
        when(repository.findByArtistId(ARTIST_ID)).thenReturn(List.of(existing));
        when(mapper.toResponse(existing)).thenReturn(response);

        List<ArtistSocialLinkResponse> actual = service.list(ARTIST_ID);

        assertThat(actual).containsExactly(response);
    }

    @Test
    @DisplayName("update succeeds for matching artist and unique platform")
    void updateSuccess() {
        ArtistSocialLink existing = link(ARTIST_ID);
        ArtistSocialLinkResponse response = new ArtistSocialLinkResponse();
        ArtistSocialLinkUpdateRequest request = updateRequest();
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(USER_ID)));
        when(repository.findById(LINK_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByArtistIdAndPlatformAndLinkIdNot(ARTIST_ID, SocialPlatform.YOUTUBE, LINK_ID)).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(response);

        ArtistSocialLinkResponse actual = service.update(ARTIST_ID, LINK_ID, request);

        assertThat(actual).isSameAs(response);
        verify(mapper).updateEntity(existing, request);
    }

    @Test
    @DisplayName("update throws not found when link missing")
    void updateLinkMissing() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(USER_ID)));
        when(repository.findById(LINK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ARTIST_ID, LINK_ID, updateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(LINK_NOT_FOUND);
    }

    @Test
    @DisplayName("update throws not found when link belongs to different artist")
    void updateWrongArtist() {
        ArtistSocialLink existing = link(999L);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(USER_ID)));
        when(repository.findById(LINK_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(ARTIST_ID, LINK_ID, updateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(LINK_NOT_FOUND);
    }

    @Test
    @DisplayName("update throws conflict for duplicate platform")
    void updateDuplicatePlatform() {
        ArtistSocialLink existing = link(ARTIST_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(USER_ID)));
        when(repository.findById(LINK_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByArtistIdAndPlatformAndLinkIdNot(ARTIST_ID, SocialPlatform.YOUTUBE, LINK_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(ARTIST_ID, LINK_ID, updateRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage(DUPLICATE_PLATFORM);
    }

    @Test
    @DisplayName("delete removes social link when ownership and artist match")
    void deleteSuccess() {
        ArtistSocialLink existing = link(ARTIST_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(USER_ID)));
        when(repository.findById(LINK_ID)).thenReturn(Optional.of(existing));

        service.delete(ARTIST_ID, LINK_ID);

        verify(repository).delete(existing);
    }

    @Test
    @DisplayName("delete throws not found when link missing")
    void deleteLinkMissing() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(USER_ID)));
        when(repository.findById(LINK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ARTIST_ID, LINK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(LINK_NOT_FOUND);
    }

    @Test
    @DisplayName("delete throws artist not found when ownership check fails")
    void deleteOwnershipFail() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(OTHER_USER_ID)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);

        assertThatThrownBy(() -> service.delete(ARTIST_ID, LINK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ARTIST_NOT_FOUND);
    }

    private Artist artist(Long userId) {
        Artist artist = new Artist();
        artist.setArtistId(ARTIST_ID);
        artist.setUserId(userId);
        return artist;
    }

    private ArtistSocialLink link(Long artistId) {
        ArtistSocialLink link = new ArtistSocialLink();
        link.setLinkId(LINK_ID);
        link.setArtistId(artistId);
        link.setPlatform(SocialPlatform.INSTAGRAM);
        return link;
    }

    private ArtistSocialLinkCreateRequest createRequest() {
        ArtistSocialLinkCreateRequest request = new ArtistSocialLinkCreateRequest();
        request.setPlatform(SocialPlatform.INSTAGRAM);
        request.setUrl("https://instagram.com/x");
        return request;
    }

    private ArtistSocialLinkUpdateRequest updateRequest() {
        ArtistSocialLinkUpdateRequest request = new ArtistSocialLinkUpdateRequest();
        request.setPlatform(SocialPlatform.YOUTUBE);
        request.setUrl("https://youtube.com/x");
        return request;
    }
}
