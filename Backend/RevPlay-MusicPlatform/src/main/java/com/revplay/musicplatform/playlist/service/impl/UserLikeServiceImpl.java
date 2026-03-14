package com.revplay.musicplatform.playlist.service.impl;

import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.exception.DuplicateResourceException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.playlist.dto.request.LikeRequest;
import com.revplay.musicplatform.playlist.dto.response.UserLikeResponse;
import com.revplay.musicplatform.playlist.entity.UserLike;
import com.revplay.musicplatform.playlist.mapper.UserLikeMapper;
import com.revplay.musicplatform.playlist.repository.UserLikeRepository;
import com.revplay.musicplatform.playlist.service.ContentReferenceValidationService;
import com.revplay.musicplatform.playlist.service.UserLikeService;
import com.revplay.musicplatform.security.AuthContextUtil;
import com.revplay.musicplatform.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserLikeServiceImpl implements UserLikeService {

    private final UserLikeRepository userLikeRepository;
    private final UserLikeMapper userLikeMapper;
    private final AuditLogService auditLogService;
    private final AuthContextUtil authContextUtil;
    private final ContentReferenceValidationService contentReferenceValidationService;


    @Transactional
    public UserLikeResponse likeContent(LikeRequest request) {
        Long currentUserId = authContextUtil.requireCurrentUserId();
        String type = request.getLikeableType().toUpperCase();
        AuditEntityType auditEntityType = "PODCAST".equals(type) ? AuditEntityType.PODCAST : AuditEntityType.SONG;
        contentReferenceValidationService.validateLikeTargetExists(type, request.getLikeableId());

        boolean alreadyLiked = userLikeRepository.existsByUserIdAndLikeableIdAndLikeableType(
                currentUserId, request.getLikeableId(), type);

        if (alreadyLiked) {
            throw new DuplicateResourceException(
                    "User " + currentUserId + " has already liked this " + type.toLowerCase());
        }

        UserLike entity = userLikeMapper.toEntity(request, currentUserId);
        UserLike saved = userLikeRepository.save(entity);

        auditLogService.logInternal(
                AuditActionType.LIKE_ADDED,
                currentUserId,
                auditEntityType,
                request.getLikeableId(),
                "User liked " + type.toLowerCase() + " with id " + request.getLikeableId());

        log.info("User {} liked {} {}", currentUserId, type, request.getLikeableId());
        return userLikeMapper.toResponse(saved);
    }


    @Transactional
    public void unlikeContent(Long likeId) {
        Long currentUserId = authContextUtil.requireCurrentUserId();
        UserLike like = userLikeRepository.findById(likeId)
                .orElseThrow(() -> new ResourceNotFoundException("Like", likeId));

        if (!like.getUserId().equals(currentUserId)) {
            throw new com.revplay.musicplatform.exception.AccessDeniedException(
                    "You can only remove your own likes");
        }

        userLikeRepository.delete(like);

        AuditEntityType removedEntityType = "PODCAST".equalsIgnoreCase(like.getLikeableType())
                ? AuditEntityType.PODCAST : AuditEntityType.SONG;
        auditLogService.logInternal(
                AuditActionType.LIKE_REMOVED,
                currentUserId,
                removedEntityType,
                like.getLikeableId(),
                "User removed like for " + like.getLikeableType().toLowerCase() + " " + like.getLikeableId());

        log.info("User {} removed like {} for {} {}", currentUserId, likeId,
                like.getLikeableType(), like.getLikeableId());
    }


    @Transactional(readOnly = true)
    public PagedResponseDto<UserLikeResponse> getUserLikes(
            Long userId, String likeableType, int page, int size) {
        Long currentUserId = authContextUtil.requireCurrentUserId();
        if (!currentUserId.equals(userId) && !authContextUtil.hasRole(UserRole.ADMIN.name())) {
            throw new com.revplay.musicplatform.exception.AccessDeniedException(
                    "You can only view your own likes");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<UserLike> resultPage;

        if (likeableType != null && !likeableType.isBlank()) {
            resultPage = userLikeRepository.findByUserIdAndLikeableType(
                    userId, likeableType.toUpperCase(), pageable);
        } else {
            resultPage = userLikeRepository.findByUserId(userId, pageable);
        }

        Page<UserLikeResponse> responsePage = resultPage.map(userLikeMapper::toResponse);
        return PagedResponseDto.of(responsePage);
    }
}


