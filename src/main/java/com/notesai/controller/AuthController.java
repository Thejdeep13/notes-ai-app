package com.notesai.controller;

import com.notesai.dto.AuthRequest;
import com.notesai.model.User;
import com.notesai.repository.UserRepository;
import com.notesai.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody AuthRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        users.save(new User(req.username(), encoder.encode(req.password())));
        return Map.of("token", jwt.generateToken(req.username()), "username", req.username());
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody AuthRequest req) {
        User user = users.findByUsername(req.username())
                .filter(u -> encoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return Map.of("token", jwt.generateToken(user.getUsername()), "username", user.getUsername());
    }
}
