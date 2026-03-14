package com.revplay.musicplatform.catalog.util;

import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class FileStorageService {
    private static final Set<String> SONG_AUDIO_EXTENSIONS = Set.of(".mp3", ".wav", ".flac");
    private static final Set<String> PODCAST_AUDIO_EXTENSIONS = Set.of(".mp3", ".aac");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path songsPath;
    private final Path podcastsPath;
    private final Path imagesPath;

    public FileStorageService(FileStorageProperties properties) {
        Path base = Path.of(properties.getBaseDir());
        this.songsPath = base.resolve(properties.getSongsDir());
        this.podcastsPath = base.resolve(properties.getPodcastsDir());
        this.imagesPath = base.resolve(properties.getImagesDir());
        ensureDirectories();
    }


    public String storeSong(MultipartFile file) {
        return store(file, songsPath, SONG_AUDIO_EXTENSIONS, "Audio file is required");
    }


    public String storePodcast(MultipartFile file) {
        return store(file, podcastsPath, PODCAST_AUDIO_EXTENSIONS, "Audio file is required");
    }


    public Resource loadSong(String fileName) {
        return load(songsPath.resolve(fileName));
    }

    public Resource loadPodcast(String fileName) {
        return load(podcastsPath.resolve(fileName));
    }

    public String storeImage(MultipartFile file) {
        return store(file, imagesPath, IMAGE_EXTENSIONS, "Image file is required");
    }

    public Resource loadImage(String fileName) {
        return load(imagesPath.resolve(fileName));
    }

    public void deleteImageFile(String fileName) {
        deleteFile(imagesPath.resolve(fileName));
    }

    public void deleteSongFile(String fileName) {
        deleteFile(songsPath.resolve(fileName));
    }

    public void deletePodcastFile(String fileName) {
        deleteFile(podcastsPath.resolve(fileName));
    }

    private String store(MultipartFile file, Path targetDir, Set<String> allowedExtensions, String missingMessage) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(missingMessage);
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String lower = original.toLowerCase(Locale.ROOT);
        String extension = allowedExtensions.stream()
            .filter(lower::endsWith)
            .findFirst()
            .orElse(null);
        if (extension == null) {
            throw new BadRequestException("Unsupported file type");
        }
        String fileName = UUID.randomUUID() + extension;
        Path target = targetDir.resolve(fileName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Failed to store file {}", fileName, ex);
            throw new BadRequestException("Failed to store file");
        }
        return fileName;
    }

    private Resource load(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("File not found");
            }
            return new UrlResource(filePath.toUri());
        } catch (IOException ex) {
            log.error("Failed to load file {}", filePath, ex);
            throw new ResourceNotFoundException("File not found");
        }
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(songsPath);
            Files.createDirectories(podcastsPath);
            Files.createDirectories(imagesPath);
        } catch (IOException ex) {
            log.error("Failed to initialize upload folders", ex);
            throw new BadRequestException("Failed to initialize upload folders");
        }
    }

    private void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Failed to delete file {}", path, ex);
        }
    }
}
