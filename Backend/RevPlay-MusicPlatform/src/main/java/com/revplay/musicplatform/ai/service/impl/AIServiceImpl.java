package com.revplay.musicplatform.ai.service.impl;

import com.revplay.musicplatform.ai.integration.OllamaClient;
import com.revplay.musicplatform.ai.service.AIService;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AIServiceImpl implements AIService {

    private static final String SUPPORT_MESSAGE =
            "Please contact RevPlay support at revplay.support@gmail.com";

    private static final String FALLBACK_MESSAGE =
            "I'm not sure about that yet. Please explore RevPlay features or contact support.";

    private static final String SYSTEM_PROMPT =
            "You are RevPlay AI, a helpful assistant for the RevPlay music streaming platform. " +
                    "Answer only questions about RevPlay. " +
                    "Always write exactly 2 complete sentences. Never leave a sentence unfinished. " +
                    "Do not greet, do not repeat the question, do not mention any API paths or URLs. " +
                    "Speak naturally as if explaining to a regular app user. " +

                    "REGISTRATION AND LOGIN: " +
                    "To register, open the RevPlay app and tap Sign Up, then enter your name, email, and password. " +
                    "After registering, check your email for an OTP code and verify your account to activate it. " +
                    "To login, enter your email and password on the login screen. " +
                    "If you forgot your password, tap Forgot Password and follow the steps sent to your email. " +

                    "SONGS: " +
                    "Only artists can upload songs by going to the Artist Dashboard and tapping Upload Song, then choosing an MP3 or WAV file. " +
                    "Artists can set songs to Public or Private and can update or delete their songs anytime. " +

                    "PLAYLISTS: " +
                    "To create a playlist, go to your Library and tap New Playlist, then give it a name. " +
                    "You can add songs, reorder them, make playlists public or private, and follow other users playlists. " +

                    "LIKES: " +
                    "To like a song or podcast, tap the heart icon on any song or podcast page. " +
                    "View all your liked content in your Library under Liked Songs. " +

                    "PODCASTS: " +
                    "Artists can create and publish podcasts from their Artist Dashboard. " +
                    "Users can browse and listen to recommended podcasts from the home screen. " +

                    "PREMIUM: " +
                    "To upgrade to premium, go to Settings and tap Upgrade to Premium, then choose Monthly or Yearly plan. " +
                    "Premium removes all ads, enables song downloads for offline listening, and unlocks exclusive content. " +

                    "ARTIST DASHBOARD: " +
                    "Artists can view play counts, top songs, listener stats, and revenue from their dashboard. " +
                    "Artists can manage their profile, add social links, and manage all uploaded content from there. " +

                    "SEARCH: " +
                    "Use the Search tab to find songs, artists, albums, and podcasts by name or genre. " +

                    "PLAYBACK AND QUEUE: " +
                    "Tap any song to play it and add songs to your queue from any song page. " +
                    "Autoplay will suggest and play similar songs when your queue ends. " +

                    "DOWNLOADS: " +
                    "Premium users can download songs for offline listening by tapping the download icon on any song. ";

    private final OllamaClient ollamaClient;

    public AIServiceImpl(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public String getAIResponse(String userPrompt) {

        if (isSupportQuery(userPrompt)) {
            return SUPPORT_MESSAGE;
        }

        String finalPrompt = "<|system|>\n"
                + SYSTEM_PROMPT + "\n</s>\n"
                + "<|user|>\n"
                + userPrompt.trim() + "\n</s>\n"
                + "<|assistant|>\n";

        String response = ollamaClient.generateResponse(finalPrompt);

        if (response == null || response.isBlank()) {
            return FALLBACK_MESSAGE;
        }

        String cleanedResponse = cleanResponse(response);

        return cleanedResponse.isBlank() ? FALLBACK_MESSAGE : cleanedResponse;
    }

    private boolean isSupportQuery(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return false;
        }
        String normalizedPrompt = userPrompt.toLowerCase(Locale.ROOT);
        return normalizedPrompt.contains("contact support")
                || normalizedPrompt.contains("support email")
                || normalizedPrompt.contains("customer support")
                || normalizedPrompt.contains("help email")
                || normalizedPrompt.contains("support team");
    }

    private String cleanResponse(String aiResponse) {

        String response = aiResponse
                .replace("\r", " ")
                .replaceAll("\\n+", " ")
                // Strip leaked TinyLlama template tokens
                .replaceAll("(?i)<\\|system\\|>.*?</s>", "")
                .replaceAll("(?i)<\\|user\\|>.*?</s>", "")
                .replaceAll("(?i)<\\|assistant\\|>", "")
                .replaceAll("</s>", "")
                // Strip role labels
                .replaceAll("(?i)^system:\\s*", "")
                .replaceAll("(?i)^assistant:\\s*", "")
                .replaceAll("(?i)^revplay ai\\s*:?\\s*", "")
                .replaceAll("(?i)^here'?s\\s+a\\s+(clear\\s+)?answer\\s*:?\\s*", "")
                // Strip numbered lists
                .replaceAll("\\b\\d+\\s*[.)]\\s*", "")
                .replace("User question", "")
                .replace("Assistant response", "")
                .replace("User:", "")
                .replace("Assistant:", "")
                .replaceAll("\\s+", " ")
                .trim();

        // Sentence-aware truncation — if response was cut off mid-sentence,
        // trim back to the last complete sentence ending with . ! or ?
        // This prevents responses like "allows you to log in with either you r."
        if (!response.isEmpty()) {
            int lastPunctuation = Math.max(
                    response.lastIndexOf('.'),
                    Math.max(response.lastIndexOf('!'), response.lastIndexOf('?'))
            );
            if (lastPunctuation > 0 && lastPunctuation < response.length() - 1) {
                // There is text after the last punctuation — cut it off cleanly
                response = response.substring(0, lastPunctuation + 1);
            } else if (!response.matches(".*[.!?]$")) {
                response = response + ".";
            }
        }

        return response;
    }
}
