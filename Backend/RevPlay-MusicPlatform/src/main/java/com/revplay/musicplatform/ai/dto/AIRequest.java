package com.revplay.musicplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIRequest {

    @NotBlank(message = "Prompt is required")
    private String prompt;
}
