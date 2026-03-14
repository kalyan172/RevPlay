package com.revplay.musicplatform.download.service;

import org.springframework.core.io.Resource;

public interface SongFileResolver {

    Resource loadSongResource(String fileUrl);
}

