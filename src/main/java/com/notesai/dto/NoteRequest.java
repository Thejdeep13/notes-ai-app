package com.notesai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String content,
        @Size(max = 255) String tags) {
}
