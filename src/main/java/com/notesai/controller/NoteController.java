package com.notesai.controller;

import com.notesai.dto.NoteRequest;
import com.notesai.dto.NoteResponse;
import com.notesai.model.Note;
import com.notesai.model.User;
import com.notesai.repository.NoteRepository;
import com.notesai.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteRepository notes;
    private final UserRepository users;

    public NoteController(NoteRepository notes, UserRepository users) {
        this.notes = notes;
        this.users = users;
    }

    @GetMapping
    public List<NoteResponse> list(Authentication auth) {
        return notes.findByOwnerUsernameOrderByCreatedAtDesc(auth.getName())
                .stream().map(NoteResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody NoteRequest req, Authentication auth) {
        User owner = users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        Note n = new Note();
        n.setTitle(req.title());
        n.setContent(req.content());
        n.setTags(req.tags());
        n.setOwner(owner);
        return NoteResponse.from(notes.save(n));
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable Long id, @Valid @RequestBody NoteRequest req, Authentication auth) {
        Note n = findOwned(id, auth);
        n.setTitle(req.title());
        n.setContent(req.content());
        if (req.tags() != null) {
            n.setTags(req.tags());
        }
        return NoteResponse.from(notes.save(n));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        notes.delete(findOwned(id, auth));
        return ResponseEntity.noContent().build();
    }

    /** Ownership check: a user can only touch their own notes. */
    private Note findOwned(Long id, Authentication auth) {
        return notes.findByIdAndOwnerUsername(id, auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
    }
}
