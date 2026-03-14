package com.revplay.musicplatform.catalog.util;

import com.mpatric.mp3agic.Mp3File;
import com.revplay.musicplatform.exception.BadRequestException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class AudioMetadataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioMetadataService.class);

    public Integer resolveDurationSeconds(MultipartFile file, Integer fallbackDurationSeconds) {
        Integer extracted = extractDurationSeconds(file);
        if (extracted != null && extracted > 0) {
            return extracted;
        }
        if (fallbackDurationSeconds != null && fallbackDurationSeconds > 0) {
            return fallbackDurationSeconds;
        }
        throw new BadRequestException("Unable to determine audio duration; provide durationSeconds in metadata");
    }

    private Integer extractDurationSeconds(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (".mp3".equals(extension)) {
            Integer mp3Seconds = extractMp3Duration(file);
            if (mp3Seconds != null) {
                return mp3Seconds;
            }
        }
        return extractByJavaSound(file);
    }

    private Integer extractMp3Duration(MultipartFile file) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("revplay-audio-", ".mp3");
            try (InputStream inputStream = file.getInputStream()) {
                FileUtils.copyInputStreamToFile(inputStream, tempFile.toFile());
            }
            Mp3File mp3File = new Mp3File(tempFile.toString());
            long seconds = mp3File.getLengthInSeconds();
            return seconds > 0 ? (int) seconds : null;
        } catch (Exception exception) {
            LOGGER.debug("MP3 duration extraction failed: {}", exception.getMessage());
            return null;
        } finally {
            if (tempFile != null) {
                FileUtils.deleteQuietly(tempFile.toFile());
            }
        }
    }

    private Integer extractByJavaSound(MultipartFile file) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("revplay-audio-", ".bin");
            try (InputStream inputStream = file.getInputStream()) {
                FileUtils.copyInputStreamToFile(inputStream, tempFile.toFile());
            }
            AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(tempFile.toFile());
            Object durationMicros = audioFileFormat.properties().get("duration");
            if (durationMicros instanceof Long duration) {
                long seconds = Math.round(duration / 1_000_000.0d);
                return seconds > 0 ? (int) seconds : null;
            }
            return null;
        } catch (Exception exception) {
            LOGGER.debug("Generic audio duration extraction failed: {}", exception.getMessage());
            return null;
        } finally {
            if (tempFile != null) {
                FileUtils.deleteQuietly(tempFile.toFile());
            }
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
