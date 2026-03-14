package com.revplay.musicplatform.playlist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.exception.AccessDeniedException;
import com.revplay.musicplatform.exception.DuplicateResourceException;
import com.revplay.musicplatform.playlist.dto.request.LikeRequest;
import com.revplay.musicplatform.playlist.dto.response.UserLikeResponse;
import com.revplay.musicplatform.playlist.entity.UserLike;
import com.revplay.musicplatform.playlist.mapper.UserLikeMapper;
import com.revplay.musicplatform.playlist.repository.UserLikeRepository;
import com.revplay.musicplatform.playlist.service.ContentReferenceValidationService;
import com.revplay.musicplatform.security.AuthContextUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserLikeServiceImplTest {

    private static final Long USER_ID = 11L;
    private static final Long OTHER_USER_ID = 22L;
    private static final Long LIKE_ID = 33L;
    private static final Long TARGET_ID = 44L;

    @Mock
    private UserLikeRepository userLikeRepository;
    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AuthContextUtil authContextUtil;
    @Mock
    private ContentReferenceValidationService contentReferenceValidationService;

    @InjectMocks
    private UserLikeServiceImpl service;

    @Test
    @DisplayName("likeContent creates like when not already liked")
    void likeContentHappyPath() {
        LikeRequest request = likeRequest("SONG");
        UserLike entity = userLike(LIKE_ID, USER_ID, TARGET_ID, "SONG");
        UserLikeResponse response = likeResponse(LIKE_ID, USER_ID, TARGET_ID, "SONG");
        when(authContextUtil.requireCurrentUserId()).thenReturn(USER_ID);
        when(userLikeRepository.existsByUserIdAndLikeableIdAndLikeableType(USER_ID, TARGET_ID, "SONG")).thenReturn(false);
        when(userLikeMapper.toEntity(request, USER_ID)).thenReturn(entity);
        when(userLikeRepository.save(entity)).thenReturn(entity);
        when(userLikeMapper.toResponse(entity)).thenReturn(response);

        UserLikeResponse actual = service.likeContent(request);

        verify(contentReferenceValidationService).validateLikeTargetExists("SONG", TARGET_ID);
        verify(auditLogService).logInternal(AuditActionType.LIKE_ADDED, USER_ID, AuditEntityType.SONG, TARGET_ID,
                "User liked song with id " + TARGET_ID);
        assertThat(actual.getLikeableType()).isEqualTo("SONG");
    }

    @Test
    @DisplayName("likeContent duplicate like throws duplicate resource")
    void likeContentDuplicate() {
        LikeRequest request = likeRequest("PODCAST");
        when(authContextUtil.requireCurrentUserId()).thenReturn(USER_ID);
        when(userLikeRepository.existsByUserIdAndLikeableIdAndLikeableType(USER_ID, TARGET_ID, "PODCAST")).thenReturn(true);

        assertThatThrownBy(() -> service.likeContent(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("unlikeContent happy path deletes like and logs audit")
    void unlikeContentHappyPath() {
        UserLike like = userLike(LIKE_ID, USER_ID, TARGET_ID, "SONG");
        when(authContextUtil.requireCurrentUserId()).thenReturn(USER_ID);
        when(userLikeRepository.findById(LIKE_ID)).thenReturn(Optional.of(like));

        service.unlikeContent(LIKE_ID);

        verify(userLikeRepository).delete(like);
        verify(auditLogService).logInternal(eq(AuditActionType.LIKE_REMOVED), eq(USER_ID), eq(AuditEntityType.SONG),
                eq(TARGET_ID), any(String.class));
    }

    @Test
    @DisplayName("unlikeContent for other user like throws access denied")
    void unlikeContentOtherUserDenied() {
        UserLike like = userLike(LIKE_ID, OTHER_USER_ID, TARGET_ID, "SONG");
        when(authContextUtil.requireCurrentUserId()).thenReturn(USER_ID);
        when(userLikeRepository.findById(LIKE_ID)).thenReturn(Optional.of(like));

        assertThatThrownBy(() -> service.unlikeContent(LIKE_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only remove your own likes");
    }

    @Test
    @DisplayName("getUserLikes returns paged likes for user")
    void getUserLikes() {
        UserLike like = userLike(LIKE_ID, USER_ID, TARGET_ID, "SONG");
        UserLikeResponse response = likeResponse(LIKE_ID, USER_ID, TARGET_ID, "SONG");
        PageRequest pageable = PageRequest.of(0, 10);
        Page<UserLike> page = new PageImpl<>(java.util.List.of(like), pageable, 1);
        when(authContextUtil.requireCurrentUserId()).thenReturn(USER_ID);
        when(userLikeRepository.findByUserId(USER_ID, pageable)).thenReturn(page);
        when(userLikeMapper.toResponse(like)).thenReturn(response);

        PagedResponseDto<UserLikeResponse> actual = service.getUserLikes(USER_ID, null, 0, 10);

        assertThat(actual.getContent()).hasSize(1);
        assertThat(actual.getContent().get(0).getLikeableId()).isEqualTo(TARGET_ID);
    }

    @Test
    @DisplayName("getUserLikes denies non-admin requesting other user likes")
    void getUserLikesDenied() {
        when(authContextUtil.requireCurrentUserId()).thenReturn(USER_ID);
        when(authContextUtil.hasRole("ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> service.getUserLikes(OTHER_USER_ID, null, 0, 10))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("isLiked style check via repository exists call returns true and false")
    void repositoryExistsCheck() {
        when(userLikeRepository.existsByUserIdAndLikeableIdAndLikeableType(USER_ID, TARGET_ID, "SONG")).thenReturn(true, false);
        assertThat(userLikeRepository.existsByUserIdAndLikeableIdAndLikeableType(USER_ID, TARGET_ID, "SONG")).isTrue();
        assertThat(userLikeRepository.existsByUserIdAndLikeableIdAndLikeableType(USER_ID, TARGET_ID, "SONG")).isFalse();
    }

    private LikeRequest likeRequest(String type) {
        LikeRequest request = new LikeRequest();
        request.setLikeableId(TARGET_ID);
        request.setLikeableType(type);
        return request;
    }

    private UserLike userLike(Long id, Long userId, Long likeableId, String type) {
        return UserLike.builder()
                .id(id)
                .userId(userId)
                .likeableId(likeableId)
                .likeableType(type)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserLikeResponse likeResponse(Long id, Long userId, Long likeableId, String type) {
        return UserLikeResponse.builder()
                .id(id)
                .userId(userId)
                .likeableId(likeableId)
                .likeableType(type)
                .build();
    }
}
