package com.revplay.musicplatform.playback.validation;

import com.revplay.musicplatform.playback.dto.request.QueueAddRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class QueueContentSelectionValidator implements ConstraintValidator<ValidQueueContentSelection, QueueAddRequest> {

    @Override
    public boolean isValid(QueueAddRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        boolean hasSong = value.songId() != null;
        boolean hasEpisode = value.episodeId() != null;
        return hasSong ^ hasEpisode;
    }
}

