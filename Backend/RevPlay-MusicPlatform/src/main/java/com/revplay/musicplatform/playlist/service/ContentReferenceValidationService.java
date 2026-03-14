package com.revplay.musicplatform.playlist.service;

public interface ContentReferenceValidationService {

    void validateLikeTargetExists(String likeableType, Long likeableId);
}
