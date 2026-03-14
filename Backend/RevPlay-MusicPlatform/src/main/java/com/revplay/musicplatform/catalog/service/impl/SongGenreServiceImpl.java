package com.revplay.musicplatform.catalog.service.impl;

import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.entity.SongGenre;
import com.revplay.musicplatform.catalog.repository.GenreRepository;
import com.revplay.musicplatform.catalog.repository.SongGenreRepository;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.service.SongGenreService;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.user.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SongGenreServiceImpl implements SongGenreService {
    private static final Logger log = LoggerFactory.getLogger(SongGenreServiceImpl.class);
    private final SongGenreRepository songGenreRepository;
    private final SongRepository songRepository;
    private final GenreRepository genreRepository;
    private final SecurityUtil securityUtil;
    private final AccessValidator accessValidator;
    private final ArtistRepository artistRepository;

    public SongGenreServiceImpl(SongGenreRepository songGenreRepository, SongRepository songRepository,
                                GenreRepository genreRepository, SecurityUtil securityUtil,
                                AccessValidator accessValidator, ArtistRepository artistRepository) {
        this.songGenreRepository = songGenreRepository;
        this.songRepository = songRepository;
        this.genreRepository = genreRepository;
        this.securityUtil = securityUtil;
        this.accessValidator = accessValidator;
        this.artistRepository = artistRepository;
    }

    @Override
    @Transactional
    public void addGenres(Long songId, List<Long> genreIds) {
        log.info("Adding genres for songId={} with {} genres", songId, genreIds.size());
        Song song = validateOwnedSongAndGenres(songId, genreIds);
        List<Long> uniqueRequested = genreIds.stream().distinct().toList();
        List<Long> existingGenreIds = songGenreRepository.findBySongId(songId).stream()
            .map(SongGenre::getGenreId)
            .collect(Collectors.toList());
        for (Long genreId : uniqueRequested) {
            if (!existingGenreIds.contains(genreId)
                    && !songGenreRepository.existsBySongIdAndGenreId(songId, genreId)) {
                SongGenre sg = new SongGenre();
                sg.setSongId(song.getSongId());
                sg.setGenreId(genreId);
                songGenreRepository.save(sg);
            }
        }
    }

    @Override
    @Transactional
    public void replaceGenres(Long songId, List<Long> genreIds) {
        log.info("Replacing genres for songId={} with {} genres", songId, genreIds.size());
        Song song = validateOwnedSongAndGenres(songId, genreIds);
        List<Long> uniqueGenreIds = genreIds.stream().distinct().toList();
        songGenreRepository.deleteBySongId(songId);
        for (Long genreId : uniqueGenreIds) {
            SongGenre sg = new SongGenre();
            sg.setSongId(song.getSongId());
            sg.setGenreId(genreId);
            songGenreRepository.save(sg);
        }
    }

    private Song validateOwnedSongAndGenres(Long songId, List<Long> genreIds) {
        accessValidator.requireArtistOrAdmin(securityUtil.getUserRole());
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new ResourceNotFoundException("Song not found"));
        String role = securityUtil.getUserRole();
        if (!UserRole.ADMIN.name().equalsIgnoreCase(role)) {
            artistRepository.findById(song.getArtistId())
                .filter(a -> a.getUserId().equals(securityUtil.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Song not found"));
        }
        long count = genreRepository.countByGenreIdIn(genreIds);
        if (count != genreIds.size()) {
            throw new BadRequestException("Invalid genre ids");
        }
        return song;
    }
}
