package com.jobconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobconnect.dto.request.LoginRequest;
import com.jobconnect.dto.request.RegisterRequest;
import com.jobconnect.dto.response.AuthResponse;
import com.jobconnect.entity.User;
import com.jobconnect.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController — Integration Tests")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AuthService authService;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = AuthResponse.builder()
                .token("mock.jwt.token").tokenType("Bearer")
                .userId(1L).email("jane@test.com")
                .fullName("Jane Seeker").role(User.Role.JOB_SEEKER)
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register — 201 Created on valid input")
    void register_validRequest_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Seeker");
        req.setEmail("jane@test.com");
        req.setPassword("secure123");
        req.setRole(User.Role.JOB_SEEKER);

        when(authService.register(any())).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.role").value("JOB_SEEKER"));
    }

    @Test
    @DisplayName("POST /api/auth/register — 400 Bad Request on missing fields")
    void register_missingFields_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest(); // empty

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login — 200 OK with valid credentials")
    void login_validRequest_returns200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("jane@test.com");
        req.setPassword("secure123");

        when(authService.login(any())).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("jane@test.com"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }
}
