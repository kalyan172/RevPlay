package com.revplay.musicplatform.playback.controller;

import com.revplay.musicplatform.playback.dto.request.QueueAddRequest;
import com.revplay.musicplatform.playback.dto.request.QueueReorderRequest;
import com.revplay.musicplatform.playback.dto.response.QueueItemResponse;
import com.revplay.musicplatform.playback.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queue")
@Tag(name = "Queue", description = "Playback queue management APIs")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping
    @Operation(summary = "Add a song or episode to queue")
    public ResponseEntity<QueueItemResponse> addToQueue(@Valid @RequestBody QueueAddRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(queueService.addToQueue(request));
    }

    @DeleteMapping("/{queueId}")
    @Operation(summary = "Remove queue item")
    public ResponseEntity<Void> removeFromQueue(@PathVariable Long queueId) {
        queueService.removeFromQueue(queueId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder queue items")
    public ResponseEntity<List<QueueItemResponse>> reorderQueue(@Valid @RequestBody QueueReorderRequest request) {
        return ResponseEntity.ok(queueService.reorder(request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get queue for a user")
    public ResponseEntity<List<QueueItemResponse>> getQueue(@PathVariable Long userId) {
        return ResponseEntity.ok(queueService.getQueue(userId));
    }

    @GetMapping("/{userId}/next")
    @Operation(summary = "Get next queue item")
    public ResponseEntity<QueueItemResponse> next(
            @PathVariable Long userId,
            @RequestParam Long currentQueueId
    ) {
        return ResponseEntity.ok(queueService.next(userId, currentQueueId));
    }

    @GetMapping("/{userId}/previous")
    @Operation(summary = "Get previous queue item")
    public ResponseEntity<QueueItemResponse> previous(
            @PathVariable Long userId,
            @RequestParam Long currentQueueId
    ) {
        return ResponseEntity.ok(queueService.previous(userId, currentQueueId));
    }
}




