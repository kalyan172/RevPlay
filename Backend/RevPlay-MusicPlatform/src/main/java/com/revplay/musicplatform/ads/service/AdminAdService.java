package com.revplay.musicplatform.ads.service;

import com.revplay.musicplatform.ads.entity.Ad;
import org.springframework.web.multipart.MultipartFile;

public interface AdminAdService {

    Ad uploadAd(String title, MultipartFile file, Integer durationSeconds);

    Ad deactivateAd(Long id);

    Ad activateAd(Long id);
}
