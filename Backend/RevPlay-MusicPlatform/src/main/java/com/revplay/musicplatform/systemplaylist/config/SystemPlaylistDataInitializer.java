package com.revplay.musicplatform.systemplaylist.config;

import com.revplay.musicplatform.systemplaylist.entity.SystemPlaylist;
import com.revplay.musicplatform.systemplaylist.repository.SystemPlaylistRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SystemPlaylistDataInitializer implements CommandLineRunner {

    private final SystemPlaylistRepository systemPlaylistRepository;

    public SystemPlaylistDataInitializer(SystemPlaylistRepository systemPlaylistRepository) {
        this.systemPlaylistRepository = systemPlaylistRepository;
    }

    @Override
    public void run(String... args) {
        if (systemPlaylistRepository.count() > 0) {
            return;
        }

        seedIfMissing("Telugu Mix", "telugu-mix", "Top Telugu tracks mixed by RevPlay");
        seedIfMissing("Tamil Mix", "tamil-mix", "Top Tamil tracks mixed by RevPlay");
        seedIfMissing("Hindi Mix", "hindi-mix", "Top Hindi tracks mixed by RevPlay");
        seedIfMissing("English Mix", "english-mix", "Top English tracks mixed by RevPlay");
        seedIfMissing("DJ Mix", "dj-mix", "High-energy DJ mix tracks from RevPlay");
    }

    private void seedIfMissing(String name, String slug, String description) {
        if (systemPlaylistRepository.findBySlugAndDeletedAtIsNull(slug).isPresent()) {
            return;
        }
        SystemPlaylist playlist = new SystemPlaylist();
        playlist.setName(name);
        playlist.setSlug(slug);
        playlist.setDescription(description);
        playlist.setIsActive(true);
        systemPlaylistRepository.save(playlist);
    }
}

