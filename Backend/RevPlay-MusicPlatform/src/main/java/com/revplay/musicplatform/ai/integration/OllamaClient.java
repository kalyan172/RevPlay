package com.revplay.musicplatform.ai.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OllamaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaClient.class);
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "tinyllama";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaClient(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public String generateResponse(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", true);

        Map<String, Object> options = new HashMap<>();
        options.put("num_predict", 160);        // enough for 2-3 complete sentences
        options.put("temperature", 0.3);
        options.put("repeat_penalty", 1.3);
        options.put("top_p", 0.9);
        options.put("top_k", 40);
        options.put("stop", new String[]{
                "</s>", "<|user|>", "<|system|>", "User:", "Assistant:"
        });
        requestBody.put("options", options);

        try {
            return restTemplate.execute(
                    OLLAMA_URL,
                    HttpMethod.POST,
                    request -> writeRequestBody(request, headers, requestBody),
                    response -> readStreamingResponse(response.getBody())
            );
        } catch (RestClientException exception) {
            LOGGER.error("Failed to fetch response from Ollama", exception);
            return null;
        }
    }

    private void writeRequestBody(
            org.springframework.http.client.ClientHttpRequest request,
            HttpHeaders headers,
            Map<String, Object> requestBody
    ) throws IOException {
        request.getHeaders().addAll(headers);
        try {
            byte[] payload = objectMapper.writeValueAsBytes(requestBody);
            request.getBody().write(payload);
        } catch (JsonProcessingException exception) {
            throw new IOException("Failed to serialize Ollama request body", exception);
        }
    }

    private String readStreamingResponse(java.io.InputStream body) throws IOException {
        if (body == null) {
            return null;
        }
        StringBuilder responseText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Map chunk = objectMapper.readValue(trimmed, Map.class);
                Object token = chunk.get("response");
                if (token instanceof String tokenText) {
                    // Strip backslash-escaped quotes AT TOKEN LEVEL before accumulating
                    // This is the root cause of \" appearing — Jackson returns raw tokens
                    // containing escape sequences that must be cleaned here, not later.
                    String cleanToken = tokenText
                            .replace("\\\"", "\"")
                            .replace("\\/", "/");
                    responseText.append(cleanToken);
                }
            }
        }
        String result = responseText.toString().trim();
        return result.isBlank() ? null : result;
    }
}
