package com.revplay.musicplatform.ai.controller;

import com.revplay.musicplatform.ai.dto.AIRequest;
import com.revplay.musicplatform.ai.dto.AIResponse;
import com.revplay.musicplatform.ai.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Assistant", description = "RevPlay AI assistant APIs")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with the RevPlay AI assistant")
    public ResponseEntity<AIResponse> chat(@Valid @RequestBody AIRequest request) {
        return ResponseEntity.ok(new AIResponse(aiService.getAIResponse(request.getPrompt())));
    }
}
