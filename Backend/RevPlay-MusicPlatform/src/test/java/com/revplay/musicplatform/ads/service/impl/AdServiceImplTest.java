package com.revplay.musicplatform.ads.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.entity.AdImpression;
import com.revplay.musicplatform.ads.entity.UserAdPlaybackState;
import com.revplay.musicplatform.ads.repository.AdImpressionRepository;
import com.revplay.musicplatform.ads.repository.AdRepository;
import com.revplay.musicplatform.ads.repository.UserAdPlaybackStateRepository;
import com.revplay.musicplatform.exception.BadRequestException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long SONG_ID_1 = 101L;
    private static final Long SONG_ID_2 = 102L;
    private static final Long SONG_ID_3 = 103L;
    private static final Long SONG_ID_4 = 104L;
    private static final Long SONG_ID_5 = 105L;
    private static final Long SONG_ID_6 = 106L;
    private static final Long AD_ID_1 = 1L;
    private static final Long AD_ID_2 = 2L;
    private static final Long AD_ID_7 = 7L;
    private static final Long MISSING_LAST_SERVED_AD_ID = 999L;
    private static final Integer SONGS_PLAYED_TWO = 2;
    private static final Integer SONGS_PLAYED_FIVE = 5;
    private static final String BAD_REQUEST_MESSAGE = "userId and songId are required";

    @Mock
    private AdRepository adRepository;

    @Mock
    private AdImpressionRepository adImpressionRepository;

    @Mock
    private UserAdPlaybackStateRepository userAdPlaybackStateRepository;

    @InjectMocks
    private AdServiceImpl service;

    @ParameterizedTest
    @MethodSource("invalidRequestProvider")
    @DisplayName("getNextAd rejects invalid identifier combinations")
    void getNextAdRejectsInvalidIdentifierCombinations(Long userId, Long songId) {
        assertThatThrownBy(() -> service.getNextAd(userId, songId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(BAD_REQUEST_MESSAGE);
    }

    @Test
    @DisplayName("getNextAd creates state for new user and skips ad before third song")
    void getNextAdCreatesStateAndSkipsBeforeThirdSong() {
        when(userAdPlaybackStateRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userAdPlaybackStateRepository.save(any(UserAdPlaybackState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ad selected = service.getNextAd(USER_ID, SONG_ID_1);

        assertThat(selected).isNull();
        ArgumentCaptor<UserAdPlaybackState> stateCaptor = ArgumentCaptor.forClass(UserAdPlaybackState.class);
        verify(userAdPlaybackStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(stateCaptor.getValue().getSongsPlayedCount()).isEqualTo(1);
        verify(adRepository, never()).findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getNextAd returns null for first two songs and records no impression")
    void getNextAdReturnsNullForFirstTwoSongs() {
        UserAdPlaybackState state = state(0, null);
        when(userAdPlaybackStateRepository.findByUserId(USER_ID)).thenReturn(Optional.of(state));
        when(userAdPlaybackStateRepository.save(any(UserAdPlaybackState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ad first = service.getNextAd(USER_ID, SONG_ID_1);
        Ad second = service.getNextAd(USER_ID, SONG_ID_2);

        assertThat(first).isNull();
        assertThat(second).isNull();
        assertThat(state.getSongsPlayedCount()).isEqualTo(2);
        verify(adImpressionRepository, never()).save(any(AdImpression.class));
    }

    @Test
    @DisplayName("getNextAd returns null on scheduled slot when no active ads exist")
    void getNextAdReturnsNullWhenNoActiveAdsExist() {
        UserAdPlaybackState state = state(SONGS_PLAYED_TWO, null);
        when(userAdPlaybackStateRepository.findByUserId(USER_ID)).thenReturn(Optional.of(state));
        when(userAdPlaybackStateRepository.save(any(UserAdPlaybackState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(adRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        Ad selected = service.getNextAd(USER_ID, SONG_ID_3);

        assertThat(selected).isNull();
        assertThat(state.getSongsPlayedCount()).isEqualTo(3);
        verify(adImpressionRepository, never()).save(any(AdImpression.class));
    }

    @Test
    @DisplayName("getNextAd rotates ads in ascending id order across scheduled slots")
    void getNextAdRotatesAdsAcrossScheduledSlots() {
        UserAdPlaybackState state = state(0, null);
        Ad adTwo = ad(AD_ID_2);
        Ad adOne = ad(AD_ID_1);
        when(userAdPlaybackStateRepository.findByUserId(USER_ID)).thenReturn(Optional.of(state));
        when(userAdPlaybackStateRepository.save(any(UserAdPlaybackState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(adRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(adTwo, adOne));

        assertThat(service.getNextAd(USER_ID, SONG_ID_1)).isNull();
        assertThat(service.getNextAd(USER_ID, SONG_ID_2)).isNull();
        assertThat(service.getNextAd(USER_ID, SONG_ID_3)).extracting(Ad::getId).isEqualTo(AD_ID_1);
        assertThat(service.getNextAd(USER_ID, SONG_ID_4)).isNull();
        assertThat(service.getNextAd(USER_ID, SONG_ID_5)).isNull();
        assertThat(service.getNextAd(USER_ID, SONG_ID_6)).extracting(Ad::getId).isEqualTo(AD_ID_2);

        assertThat(state.getLastServedAdId()).isEqualTo(AD_ID_2);
        verify(adImpressionRepository, times(2)).save(any(AdImpression.class));
    }

    @Test
    @DisplayName("getNextAd repeats the same ad when only one active ad exists")
    void getNextAdRepeatsSingleAd() {
        UserAdPlaybackState state = state(SONGS_PLAYED_TWO, null);
        Ad ad = ad(AD_ID_7);
        when(userAdPlaybackStateRepository.findByUserId(USER_ID)).thenReturn(Optional.of(state));
        when(userAdPlaybackStateRepository.save(any(UserAdPlaybackState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(adRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(ad));

        Ad firstScheduled = service.getNextAd(USER_ID, SONG_ID_3);
        state.setSongsPlayedCount(SONGS_PLAYED_FIVE);
        Ad secondScheduled = service.getNextAd(USER_ID, SONG_ID_6);

        assertThat(firstScheduled.getId()).isEqualTo(AD_ID_7);
        assertThat(secondScheduled.getId()).isEqualTo(AD_ID_7);
        assertThat(state.getLastServedAdId()).isEqualTo(AD_ID_7);
    }

    @Test
    @DisplayName("getNextAd falls back to first ad when last served id is absent from active list")
    void getNextAdFallsBackToFirstWhenLastServedIdMissing() {
        UserAdPlaybackState state = state(SONGS_PLAYED_TWO, MISSING_LAST_SERVED_AD_ID);
        Ad first = ad(AD_ID_1);
        Ad second = ad(AD_ID_2);
        when(userAdPlaybackStateRepository.findByUserId(USER_ID)).thenReturn(Optional.of(state));
        when(userAdPlaybackStateRepository.save(any(UserAdPlaybackState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(adRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(second, first));

        Ad selected = service.getNextAd(USER_ID, SONG_ID_3);

        assertThat(selected.getId()).isEqualTo(AD_ID_1);
        assertThat(state.getLastServedAdId()).isEqualTo(AD_ID_1);
    }

    @Test
    @DisplayName("getNextAd treats null songsPlayedCount as zero")
    void getNextAdTreatsNullSongsPlayedCountAsZero() {
        UserAdPlaybackState state = state(null, null);
        when(userAdPlaybackStateRepository.findByUserId(USER_ID)).thenReturn(Optional.of(state));
        when(userAdPlaybackStateRepository.save(any(UserAdPlaybackState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ad selected = service.getNextAd(USER_ID, SONG_ID_1);

        assertThat(selected).isNull();
        assertThat(state.getSongsPlayedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getNextAd persists impression details when ad is served")
    void getNextAdPersistsImpressionDetails() {
        UserAdPlaybackState state = state(SONGS_PLAYED_TWO, null);
        Ad ad = ad(AD_ID_7);
        when(userAdPlaybackStateRepository.findByUserId(USER_ID)).thenReturn(Optional.of(state));
        when(userAdPlaybackStateRepository.save(any(UserAdPlaybackState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(adRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(ad));

        Ad selected = service.getNextAd(USER_ID, SONG_ID_3);

        ArgumentCaptor<AdImpression> impressionCaptor = ArgumentCaptor.forClass(AdImpression.class);
        verify(adImpressionRepository).save(impressionCaptor.capture());
        assertThat(selected.getId()).isEqualTo(AD_ID_7);
        assertThat(impressionCaptor.getValue().getAdId()).isEqualTo(AD_ID_7);
        assertThat(impressionCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(impressionCaptor.getValue().getSongId()).isEqualTo(SONG_ID_3);
        assertThat(impressionCaptor.getValue().getPlayedAt()).isNotNull();
    }

    private static Stream<Arguments> invalidRequestProvider() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(null, SONG_ID_1),
                Arguments.of(USER_ID, null)
        );
    }

    private UserAdPlaybackState state(Integer songsPlayedCount, Long lastServedAdId) {
        UserAdPlaybackState state = new UserAdPlaybackState();
        state.setUserId(USER_ID);
        state.setSongsPlayedCount(songsPlayedCount);
        state.setLastServedAdId(lastServedAdId);
        return state;
    }

    private Ad ad(Long id) {
        Ad ad = new Ad();
        ad.setId(id);
        ad.setTitle("Ad-" + id);
        ad.setMediaUrl("/ads/" + id + ".mp3");
        ad.setDurationSeconds(30);
        ad.setIsActive(true);
        ad.setStartDate(LocalDateTime.now().minusDays(1));
        ad.setEndDate(LocalDateTime.now().plusDays(1));
        return ad;
    }
}
