package com.revplay.musicplatform.ads.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.repository.AdRepository;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminAdServiceImplTest {

    private static final String FILE_PARAM = "file";
    private static final String MP3_NAME = "ad.mp3";
    private static final String WAV_NAME = "ad.wav";
    private static final String AUDIO_MPEG = "audio/mpeg";
    private static final String AUDIO_WAV = "audio/wav";
    private static final String AUDIO_BYTES = "audio-bytes";
    private static final String RAW_TITLE = "  Ad Title  ";
    private static final String TRIMMED_TITLE = "Ad Title";
    private static final Integer VALID_DURATION = 30;
    private static final Integer INVALID_DURATION = 0;
    private static final Long AD_ID = 1L;
    private static final String BASE_DIR_NAME = "ads-base";
    private static final String ADS_DIR_NAME = "ads";
    private static final String TITLE_REQUIRED_MESSAGE = "title is required";
    private static final String DURATION_REQUIRED_MESSAGE = "durationSeconds must be > 0";
    private static final String FILE_REQUIRED_MESSAGE = "file is required";
    private static final String MP3_ONLY_MESSAGE = "Only mp3 files are allowed";
    private static final String STORAGE_INIT_MESSAGE = "Could not initialize ads storage";
    private static final String NOT_FOUND_MESSAGE = "Ad not found: 1";

    @TempDir
    Path tempDir;

    @Mock
    private AdRepository adRepository;

    @Test
    @DisplayName("uploadAd stores mp3 file and saves active ad")
    void uploadAdStoresMp3FileAndSavesAd() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));
        MockMultipartFile file = mp3File();
        when(adRepository.save(any(Ad.class))).thenAnswer(invocation -> {
            Ad ad = invocation.getArgument(0);
            ad.setId(AD_ID);
            return ad;
        });

        Ad saved = service.uploadAd(RAW_TITLE, file, VALID_DURATION);

        assertThat(saved.getId()).isEqualTo(AD_ID);
        assertThat(saved.getTitle()).isEqualTo(TRIMMED_TITLE);
        assertThat(saved.getDurationSeconds()).isEqualTo(VALID_DURATION);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getMediaUrl()).contains("/" + tempDir.resolve(BASE_DIR_NAME));
        assertThat(saved.getMediaUrl()).contains("/" + ADS_DIR_NAME + "/");
        assertThat(saved.getStartDate()).isNotNull();
        assertThat(saved.getEndDate()).isAfter(saved.getStartDate());
        assertThat(Files.exists(tempDir.resolve(BASE_DIR_NAME).resolve(ADS_DIR_NAME))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidTitleProvider")
    @DisplayName("uploadAd rejects blank title values")
    void uploadAdRejectsBlankTitleValues(String title) {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));

        assertThatThrownBy(() -> service.uploadAd(title, mp3File(), VALID_DURATION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(TITLE_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("uploadAd rejects non positive duration")
    void uploadAdRejectsNonPositiveDuration() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));

        assertThatThrownBy(() -> service.uploadAd(TRIMMED_TITLE, mp3File(), INVALID_DURATION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(DURATION_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("uploadAd rejects null file")
    void uploadAdRejectsNullFile() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));

        assertThatThrownBy(() -> service.uploadAd(TRIMMED_TITLE, null, VALID_DURATION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(FILE_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("uploadAd rejects empty multipart file")
    void uploadAdRejectsEmptyMultipartFile() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));
        MockMultipartFile file = new MockMultipartFile(FILE_PARAM, MP3_NAME, AUDIO_MPEG, new byte[0]);

        assertThatThrownBy(() -> service.uploadAd(TRIMMED_TITLE, file, VALID_DURATION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(FILE_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("uploadAd rejects non mp3 file")
    void uploadAdRejectsNonMp3File() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));
        MockMultipartFile file = new MockMultipartFile(FILE_PARAM, WAV_NAME, AUDIO_WAV, AUDIO_BYTES.getBytes());

        assertThatThrownBy(() -> service.uploadAd(TRIMMED_TITLE, file, VALID_DURATION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(MP3_ONLY_MESSAGE);
    }

    @Test
    @DisplayName("uploadAd fails when ads storage parent path is a file")
    void uploadAdFailsWhenAdsStorageParentPathIsAFile() throws IOException {
        Path baseFile = Files.createFile(tempDir.resolve("blocked-base"));
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(baseFile));

        assertThatThrownBy(() -> service.uploadAd(TRIMMED_TITLE, mp3File(), VALID_DURATION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(STORAGE_INIT_MESSAGE);
    }

    @Test
    @DisplayName("deactivateAd marks existing ad inactive")
    void deactivateAdMarksExistingAdInactive() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));
        Ad existing = ad(true);
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(existing));
        when(adRepository.save(existing)).thenReturn(existing);

        Ad updated = service.deactivateAd(AD_ID);

        assertThat(updated.getIsActive()).isFalse();
        verify(adRepository).save(existing);
    }

    @Test
    @DisplayName("deactivateAd throws when ad is missing")
    void deactivateAdThrowsWhenAdIsMissing() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));
        when(adRepository.findById(AD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateAd(AD_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND_MESSAGE);
    }

    @Test
    @DisplayName("activateAd marks existing ad active")
    void activateAdMarksExistingAdActive() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));
        Ad existing = ad(false);
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(existing));
        when(adRepository.save(existing)).thenReturn(existing);

        Ad updated = service.activateAd(AD_ID);

        assertThat(updated.getIsActive()).isTrue();
        verify(adRepository).save(existing);
    }

    @Test
    @DisplayName("activateAd throws when ad is missing")
    void activateAdThrowsWhenAdIsMissing() {
        AdminAdServiceImpl service = new AdminAdServiceImpl(adRepository, storageProperties(tempDir.resolve(BASE_DIR_NAME)));
        when(adRepository.findById(AD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateAd(AD_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND_MESSAGE);
    }

    private static Stream<Arguments> invalidTitleProvider() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of("   ")
        );
    }

    private MockMultipartFile mp3File() {
        return new MockMultipartFile(FILE_PARAM, MP3_NAME, AUDIO_MPEG, AUDIO_BYTES.getBytes());
    }

    private FileStorageProperties storageProperties(Path baseDir) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBaseDir(baseDir.toString());
        properties.setAdsDir(ADS_DIR_NAME);
        return properties;
    }

    private Ad ad(boolean active) {
        Ad ad = new Ad();
        ad.setId(AD_ID);
        ad.setIsActive(active);
        return ad;
    }
}
