package com.resumepilot.auth.service;

import com.resumepilot.auth.dto.AuthRequest;
import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.dto.RegisterRequest;
import com.resumepilot.auth.entity.User;

public interface AuthService {
    AuthResponse register(User u);
    AuthResponse login(AuthRequest req);
	AuthResponse register(RegisterRequest req);
}