package com.revplay.musicplatform.playback.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.playback.dto.request.QueueAddRequest;
import com.revplay.musicplatform.playback.dto.request.QueueReorderRequest;
import com.revplay.musicplatform.playback.dto.response.QueueItemResponse;
import com.revplay.musicplatform.playback.entity.QueueItem;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import com.revplay.musicplatform.playback.mapper.QueueItemMapper;
import com.revplay.musicplatform.playback.repository.QueueItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class QueueServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long SONG_ID = 99L;
    private static final Long EPISODE_ID = 77L;
    private static final Long QUEUE_ID_1 = 100L;
    private static final Long QUEUE_ID_2 = 200L;
    private static final String DB_DOWN = "db down";

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private QueueItemRepository queueItemRepository;
    @Mock
    private QueueItemMapper queueItemMapper;

    private QueueServiceImpl queueService;

    @BeforeEach
    void setUp() {
        queueService = new QueueServiceImpl(jdbcTemplate, queueItemRepository, queueItemMapper);
    }

    @Test
    @DisplayName("addToQueue song only with empty queue assigns position one and saves")
    void addToQueueSongOnlyEmptyQueue() {
        QueueAddRequest request = new QueueAddRequest(USER_ID, SONG_ID, null);
        QueueItem saved = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        QueueItemResponse response = queueResponse(QUEUE_ID_1, USER_ID, SONG_ID, null, 1);

        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findTopByUserIdOrderByPositionDesc(USER_ID)).thenReturn(Optional.empty());
        when(queueItemRepository.save(any(QueueItem.class))).thenReturn(saved);
        when(queueItemMapper.toDto(saved)).thenReturn(response);

        QueueItemResponse actual = queueService.addToQueue(request);

        ArgumentCaptor<QueueItem> captor = ArgumentCaptor.forClass(QueueItem.class);
        verify(queueItemRepository).save(captor.capture());
        assertThat(captor.getValue().getPosition()).isEqualTo(1);
        assertThat(actual.position()).isEqualTo(1);
    }

    @Test
    @DisplayName("addToQueue episode only appends after last position")
    void addToQueueEpisodeOnlyWithExistingItems() {
        QueueAddRequest request = new QueueAddRequest(USER_ID, null, EPISODE_ID);
        QueueItem lastItem = queueItem(USER_ID, SONG_ID, null, 5, QUEUE_ID_1);
        QueueItem saved = queueItem(USER_ID, null, EPISODE_ID, 6, QUEUE_ID_2);

        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findTopByUserIdOrderByPositionDesc(USER_ID)).thenReturn(Optional.of(lastItem));
        when(queueItemRepository.save(any(QueueItem.class))).thenReturn(saved);
        when(queueItemMapper.toDto(saved)).thenReturn(queueResponse(QUEUE_ID_2, USER_ID, null, EPISODE_ID, 6));

        QueueItemResponse actual = queueService.addToQueue(request);

        assertThat(actual.position()).isEqualTo(6);
    }

    @Test
    @DisplayName("addToQueue with null request throws playback validation exception")
    void addToQueueNullRequest() {
        assertThatThrownBy(() -> queueService.addToQueue(null))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("userId is required");
    }

    @Test
    @DisplayName("addToQueue with both song and episode null throws playback validation exception")
    void addToQueueBothNull() {
        QueueAddRequest request = new QueueAddRequest(USER_ID, null, null);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);

        assertThatThrownBy(() -> queueService.addToQueue(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("Exactly one of songId or episodeId must be provided");
    }

    @Test
    @DisplayName("addToQueue with both song and episode set throws playback validation exception")
    void addToQueueBothSet() {
        QueueAddRequest request = new QueueAddRequest(USER_ID, SONG_ID, EPISODE_ID);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);

        assertThatThrownBy(() -> queueService.addToQueue(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("Exactly one of songId or episodeId must be provided");
    }

    @Test
    @DisplayName("addToQueue when user does not exist throws playback not found exception")
    void addToQueueUserNotInDb() {
        QueueAddRequest request = new QueueAddRequest(USER_ID, SONG_ID, null);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(0L);

        assertThatThrownBy(() -> queueService.addToQueue(request))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("User " + USER_ID + " does not exist");
    }

    @Test
    @DisplayName("addToQueue propagates data access exception")
    void addToQueueJdbcTemplateThrows() {
        QueueAddRequest request = new QueueAddRequest(USER_ID, SONG_ID, null);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID)))
                .thenThrow(new DataAccessResourceFailureException(DB_DOWN));

        assertThatThrownBy(() -> queueService.addToQueue(request))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining(DB_DOWN);
    }

    @Test
    @DisplayName("getQueue with items returns ordered list")
    void getQueueReturnsItems() {
        QueueItem first = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        QueueItem second = queueItem(USER_ID, null, EPISODE_ID, 2, QUEUE_ID_2);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdOrderByPositionAscQueueIdAsc(USER_ID)).thenReturn(List.of(first, second));
        when(queueItemMapper.toDto(first)).thenReturn(queueResponse(QUEUE_ID_1, USER_ID, SONG_ID, null, 1));
        when(queueItemMapper.toDto(second)).thenReturn(queueResponse(QUEUE_ID_2, USER_ID, null, EPISODE_ID, 2));

        List<QueueItemResponse> queue = queueService.getQueue(USER_ID);

        assertThat(queue).hasSize(2);
        assertThat(queue.get(0).position()).isEqualTo(1);
        assertThat(queue.get(1).position()).isEqualTo(2);
    }

    @Test
    @DisplayName("getQueue with empty data returns empty list")
    void getQueueEmpty() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdOrderByPositionAscQueueIdAsc(USER_ID)).thenReturn(List.of());

        List<QueueItemResponse> queue = queueService.getQueue(USER_ID);

        assertThat(queue).isEmpty();
    }

    @Test
    @DisplayName("getQueue with null user id fails validation")
    void getQueueNullUserId() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq((Long) null))).thenReturn(0L);

        assertThatThrownBy(() -> queueService.getQueue(null))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("User null does not exist");
    }

    @Test
    @DisplayName("getQueue when user count is null throws playback not found exception")
    void getQueueNullUserCount() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(null);

        assertThatThrownBy(() -> queueService.getQueue(USER_ID))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("User " + USER_ID + " does not exist");
    }

    @Test
    @DisplayName("removeFromQueue deletes when queue item exists")
    void removeFromQueueFound() {
        QueueItem entity = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        when(queueItemRepository.findById(QUEUE_ID_1)).thenReturn(Optional.of(entity));

        queueService.removeFromQueue(QUEUE_ID_1);

        verify(queueItemRepository).delete(entity);
    }

    @Test
    @DisplayName("removeFromQueue throws not found when queue item missing")
    void removeFromQueueNotFound() {
        when(queueItemRepository.findById(QUEUE_ID_1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.removeFromQueue(QUEUE_ID_1))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("Queue item " + QUEUE_ID_1 + " not found");
    }

    @Test
    @DisplayName("reorder with valid queue updates positions")
    void reorderValid() {
        QueueReorderRequest request = new QueueReorderRequest(USER_ID, List.of(QUEUE_ID_2, QUEUE_ID_1));
        QueueItem first = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        QueueItem second = queueItem(USER_ID, EPISODE_ID, null, 2, QUEUE_ID_2);

        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdForUpdate(USER_ID)).thenReturn(List.of(first, second));
        when(queueItemRepository.findByUserIdOrderByPositionAscQueueIdAsc(USER_ID)).thenReturn(List.of(second, first));
        when(queueItemMapper.toDto(second)).thenReturn(queueResponse(QUEUE_ID_2, USER_ID, EPISODE_ID, null, 1));
        when(queueItemMapper.toDto(first)).thenReturn(queueResponse(QUEUE_ID_1, USER_ID, SONG_ID, null, 2));

        List<QueueItemResponse> result = queueService.reorder(request);

        verify(queueItemRepository).saveAll(List.of(second, first));
        assertThat(result).hasSize(2);
        assertThat(second.getPosition()).isEqualTo(1);
        assertThat(first.getPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("reorder with queue item from another user fails validation")
    void reorderDifferentUserItem() {
        QueueReorderRequest request = new QueueReorderRequest(USER_ID, List.of(QUEUE_ID_1, QUEUE_ID_2));
        QueueItem first = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdForUpdate(USER_ID)).thenReturn(List.of(first));

        assertThatThrownBy(() -> queueService.reorder(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("Queue item " + QUEUE_ID_2 + " does not belong to user " + USER_ID);
    }

    @Test
    @DisplayName("reorder with duplicate queue ids fails validation")
    void reorderDuplicateQueueIds() {
        QueueReorderRequest request = new QueueReorderRequest(USER_ID, List.of(QUEUE_ID_1, QUEUE_ID_1));
        QueueItem first = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdForUpdate(USER_ID)).thenReturn(List.of(first));

        assertThatThrownBy(() -> queueService.reorder(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("queueIdsInOrder contains duplicate queue IDs");
    }

    @Test
    @DisplayName("reorder with empty queue id list fails validation")
    void reorderEmptyList() {
        QueueReorderRequest request = new QueueReorderRequest(USER_ID, List.of());

        assertThatThrownBy(() -> queueService.reorder(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("userId and queueIdsInOrder are required");

        verify(queueItemRepository, never()).findByUserIdForUpdate(any());
    }

    @Test
    @DisplayName("reorder with null request fails with null pointer exception")
    void reorderNullRequest() {
        assertThatThrownBy(() -> queueService.reorder(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("reorder with null user id fails validation")
    void reorderNullUserId() {
        QueueReorderRequest request = new QueueReorderRequest(null, List.of(QUEUE_ID_1));

        assertThatThrownBy(() -> queueService.reorder(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("userId and queueIdsInOrder are required");
    }

    @Test
    @DisplayName("next returns following queue item")
    void nextReturnsFollowingQueueItem() {
        QueueItem first = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        QueueItem second = queueItem(USER_ID, null, EPISODE_ID, 2, QUEUE_ID_2);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdOrderByPositionAscQueueIdAsc(USER_ID)).thenReturn(List.of(first, second));
        when(queueItemMapper.toDto(first)).thenReturn(queueResponse(QUEUE_ID_1, USER_ID, SONG_ID, null, 1));
        when(queueItemMapper.toDto(second)).thenReturn(queueResponse(QUEUE_ID_2, USER_ID, null, EPISODE_ID, 2));

        QueueItemResponse next = queueService.next(USER_ID, QUEUE_ID_1);

        assertThat(next.queueId()).isEqualTo(QUEUE_ID_2);
    }

    @Test
    @DisplayName("next wraps to first queue item when current is last")
    void nextWrapsToFirstQueueItem() {
        QueueItem first = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        QueueItem second = queueItem(USER_ID, null, EPISODE_ID, 2, QUEUE_ID_2);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdOrderByPositionAscQueueIdAsc(USER_ID)).thenReturn(List.of(first, second));
        when(queueItemMapper.toDto(first)).thenReturn(queueResponse(QUEUE_ID_1, USER_ID, SONG_ID, null, 1));
        when(queueItemMapper.toDto(second)).thenReturn(queueResponse(QUEUE_ID_2, USER_ID, null, EPISODE_ID, 2));

        QueueItemResponse next = queueService.next(USER_ID, QUEUE_ID_2);

        assertThat(next.queueId()).isEqualTo(QUEUE_ID_1);
    }

    @Test
    @DisplayName("previous wraps to last queue item when current is first")
    void previousWrapsToLastQueueItem() {
        QueueItem first = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        QueueItem second = queueItem(USER_ID, null, EPISODE_ID, 2, QUEUE_ID_2);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdOrderByPositionAscQueueIdAsc(USER_ID)).thenReturn(List.of(first, second));
        when(queueItemMapper.toDto(first)).thenReturn(queueResponse(QUEUE_ID_1, USER_ID, SONG_ID, null, 1));
        when(queueItemMapper.toDto(second)).thenReturn(queueResponse(QUEUE_ID_2, USER_ID, null, EPISODE_ID, 2));

        QueueItemResponse previous = queueService.previous(USER_ID, QUEUE_ID_1);

        assertThat(previous.queueId()).isEqualTo(QUEUE_ID_2);
    }

    @Test
    @DisplayName("next with null current queue id fails validation")
    void nextNullCurrentQueueId() {
        assertThatThrownBy(() -> queueService.next(USER_ID, null))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("currentQueueId is required");
    }

    @Test
    @DisplayName("previous with empty queue throws not found exception")
    void previousEmptyQueue() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdOrderByPositionAscQueueIdAsc(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> queueService.previous(USER_ID, QUEUE_ID_1))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("Queue is empty for user " + USER_ID);
    }

    @Test
    @DisplayName("next with missing queue id throws not found exception")
    void nextMissingQueueItem() {
        QueueItem first = queueItem(USER_ID, SONG_ID, null, 1, QUEUE_ID_1);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(queueItemRepository.findByUserIdOrderByPositionAscQueueIdAsc(USER_ID)).thenReturn(List.of(first));
        when(queueItemMapper.toDto(first)).thenReturn(queueResponse(QUEUE_ID_1, USER_ID, SONG_ID, null, 1));

        assertThatThrownBy(() -> queueService.next(USER_ID, QUEUE_ID_2))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("Queue item " + QUEUE_ID_2 + " not found for user " + USER_ID);
    }

    private QueueItem queueItem(Long userId, Long songId, Long episodeId, Integer position, Long queueId) {
        QueueItem item = new QueueItem();
        item.setQueueId(queueId);
        item.setUserId(userId);
        item.setSongId(songId);
        item.setEpisodeId(episodeId);
        item.setPosition(position);
        item.setCreatedAt(Instant.now());
        return item;
    }

    private QueueItemResponse queueResponse(Long queueId, Long userId, Long songId, Long episodeId, Integer position) {
        return new QueueItemResponse(queueId, userId, songId, episodeId, position, Instant.now());
    }
}
