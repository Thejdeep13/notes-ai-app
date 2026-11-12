package com.notesai.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Claude Messages API.
 * The API key comes from the ANTHROPIC_API_KEY environment variable.
 */
@Service
public class AiService {

    private final RestClient client;
    private final String apiKey;
    private final String model;

    public AiService(@Value("${anthropic.api-key}") String apiKey,
                     @Value("${anthropic.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    public String summarize(String noteContent) {
        return ask("You summarize notes in 2-3 concise sentences. Output only the summary.",
                noteContent);
    }

    public String suggestTags(String title, String noteContent) {
        return ask("Suggest 3-5 short lowercase tags for this note. "
                + "Output only the tags, comma-separated, no hashtags, no extra text.",
                title + "\n\n" + noteContent);
    }

    public String answerFromNotes(String question, String allNotes) {
        return ask("Answer the question using ONLY the user's notes below. "
                + "If the answer is not in the notes, say so plainly.\n\nNOTES:\n" + allNotes,
                question);
    }

    private String ask(String systemPrompt, String userText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is not configured. Set the ANTHROPIC_API_KEY environment variable and restart.");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 600,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userText)));
        try {
            JsonNode res = client.post().uri("/v1/messages")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = res == null ? "" : res.path("content").path(0).path("text").asText("");
            if (text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an empty response");
            }
            return text.trim();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI request failed. Check your API key, model name and network.");
        }
    }
}
