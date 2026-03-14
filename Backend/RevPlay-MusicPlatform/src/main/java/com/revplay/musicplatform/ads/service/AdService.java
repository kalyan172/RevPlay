package com.revplay.musicplatform.ads.service;

import com.revplay.musicplatform.ads.entity.Ad;

public interface AdService {

    Ad getNextAd(Long userId, Long songId);
}

