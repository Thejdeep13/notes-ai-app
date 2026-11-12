package com.notesai.controller;

import com.notesai.dto.AskRequest;
import com.notesai.model.Note;
import com.notesai.repository.NoteRepository;
import com.notesai.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final int MAX_CONTEXT_CHARS = 20_000; // keeps prompt size and cost bounded

    private final AiService ai;
    private final NoteRepository notes;

    public AiController(AiService ai, NoteRepository notes) {
        this.ai = ai;
        this.notes = notes;
    }

    @PostMapping("/notes/{id}/summarize")
    public Map<String, String> summarize(@PathVariable Long id, Authentication auth) {
        Note n = findOwned(id, auth);
        return Map.of("summary", ai.summarize(nullToEmpty(n.getContent())));
    }

    /** Generates tags and saves them on the note. */
    @PostMapping("/notes/{id}/tags")
    public Map<String, String> tags(@PathVariable Long id, Authentication auth) {
        Note n = findOwned(id, auth);
        String tags = ai.suggestTags(n.getTitle(), nullToEmpty(n.getContent()));
        if (tags.length() > 255) {
            tags = tags.substring(0, 255);
        }
        n.setTags(tags);
        notes.save(n);
        return Map.of("tags", tags);
    }

    @PostMapping("/ask")
    public Map<String, String> ask(@Valid @RequestBody AskRequest req, Authentication auth) {
        List<Note> mine = notes.findByOwnerUsernameOrderByCreatedAtDesc(auth.getName());
        if (mine.isEmpty()) {
            return Map.of("answer", "You don't have any notes yet. Add a few and ask again.");
        }
        String all = mine.stream()
                .map(n -> "- " + n.getTitle() + ": " + nullToEmpty(n.getContent()))
                .collect(Collectors.joining("\n"));
        if (all.length() > MAX_CONTEXT_CHARS) {
            all = all.substring(0, MAX_CONTEXT_CHARS);
        }
        return Map.of("answer", ai.answerFromNotes(req.question(), all));
    }

    // Ownership check: AI is only ever run on the caller's own notes.
    private Note findOwned(Long id, Authentication auth) {
        return notes.findByIdAndOwnerUsername(id, auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
