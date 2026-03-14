package com.revplay.musicplatform.ads.service.impl;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.repository.AdRepository;
import com.revplay.musicplatform.ads.service.AdminAdService;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminAdServiceImpl implements AdminAdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAdServiceImpl.class);

    private final AdRepository adRepository;
    private final FileStorageProperties fileStorageProperties;

    public AdminAdServiceImpl(AdRepository adRepository, FileStorageProperties fileStorageProperties) {
        this.adRepository = adRepository;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    @Transactional
    public Ad uploadAd(String title, MultipartFile file, Integer durationSeconds) {
        validateInput(title, file, durationSeconds);
        Path adsDir = Path.of(fileStorageProperties.getBaseDir(), fileStorageProperties.getAdsDir());

        try {
            Files.createDirectories(adsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create ads directory {}", adsDir.toAbsolutePath(), e);
            throw new BadRequestException("Could not initialize ads storage");
        }

        String filename = UUID.randomUUID() + ".mp3";
        Path targetPath = adsDir.resolve(filename);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to store ad file {}", targetPath.toAbsolutePath(), e);
            throw new BadRequestException("Could not store ad audio file");
        }

        LocalDateTime now = LocalDateTime.now();
        Ad ad = new Ad();
        ad.setTitle(title.trim());
        ad.setMediaUrl("/" + fileStorageProperties.getBaseDir() + "/" + fileStorageProperties.getAdsDir() + "/" + filename);
        ad.setDurationSeconds(durationSeconds);
        ad.setIsActive(true);
        ad.setStartDate(now);
        ad.setEndDate(now.plusMonths(6));

        Ad saved = adRepository.save(ad);
        LOGGER.info("Ad uploaded successfully: adId={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Override
    @Transactional
    public Ad deactivateAd(Long id) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ad", id));
        ad.setIsActive(false);
        Ad saved = adRepository.save(ad);
        LOGGER.info("Ad deactivated: adId={}", id);
        return saved;
    }

    @Override
    @Transactional
    public Ad activateAd(Long id) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ad", id));
        ad.setIsActive(true);
        Ad saved = adRepository.save(ad);
        LOGGER.info("Ad activated: adId={}", id);
        return saved;
    }

    private void validateInput(String title, MultipartFile file, Integer durationSeconds) {
        if (title == null || title.trim().isEmpty()) {
            throw new BadRequestException("title is required");
        }
        if (durationSeconds == null || durationSeconds <= 0) {
            throw new BadRequestException("durationSeconds must be > 0");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean isMp3 = lowerName.endsWith(".mp3") || "audio/mpeg".equals(contentType) || "audio/mp3".equals(contentType);
        if (!isMp3) {
            throw new BadRequestException("Only mp3 files are allowed");
        }
    }
}
