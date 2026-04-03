package com.jobconnect.service;

import com.jobconnect.dto.request.LoginRequest;
import com.jobconnect.dto.request.RegisterRequest;
import com.jobconnect.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
