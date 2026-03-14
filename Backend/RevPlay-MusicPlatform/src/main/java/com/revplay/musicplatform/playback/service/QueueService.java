package com.revplay.musicplatform.playback.service;

import com.revplay.musicplatform.playback.dto.request.QueueAddRequest;
import com.revplay.musicplatform.playback.dto.request.QueueReorderRequest;
import com.revplay.musicplatform.playback.dto.response.QueueItemResponse;
import java.util.List;

public interface QueueService {

    QueueItemResponse addToQueue(QueueAddRequest request);

    List<QueueItemResponse> getQueue(Long userId);

    void removeFromQueue(Long queueId);

    List<QueueItemResponse> reorder(QueueReorderRequest request);

    QueueItemResponse next(Long userId, Long currentQueueId);

    QueueItemResponse previous(Long userId, Long currentQueueId);
}


