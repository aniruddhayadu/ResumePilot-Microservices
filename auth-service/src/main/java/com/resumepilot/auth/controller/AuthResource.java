package com.resumepilot.auth.controller;

import com.resumepilot.auth.dto.AuthRequest;
import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.entity.User;
import com.resumepilot.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    private final AuthService svc;

    public AuthResource(AuthService svc) {
        this.svc = svc;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody User u) {
        return ResponseEntity.ok(svc.register(u));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        return ResponseEntity.ok(svc.login(req));
    }
}