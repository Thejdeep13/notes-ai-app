package com.notesai.dto;

import com.notesai.model.Note;

import java.time.Instant;

public record NoteResponse(Long id, String title, String content, String tags, Instant createdAt) {

    public static NoteResponse from(Note n) {
        return new NoteResponse(n.getId(), n.getTitle(), n.getContent(), n.getTags(), n.getCreatedAt());
    }
}
