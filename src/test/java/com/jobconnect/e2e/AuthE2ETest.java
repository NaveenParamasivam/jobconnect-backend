package com.jobconnect.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobconnect.dto.request.LoginRequest;
import com.jobconnect.dto.request.RegisterRequest;
import com.jobconnect.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test for the full authentication workflow.
 * Boots the full Spring context with H2 in-memory database.
 * No mocks — exercises every layer from HTTP down to DB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("E2E — Authentication Workflow")
class AuthE2ETest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    // ── Registration ─────────────────────────────────────────────

    @Test
    @DisplayName("Full flow: register JOB_SEEKER → receive JWT → verify token present")
    void registerSeeker_fullFlow_returnsJwtToken() throws Exception {
        RegisterRequest req = buildRegisterRequest(
                "Alice Seeker", "alice.e2e@test.com", "password123", User.Role.JOB_SEEKER);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("JOB_SEEKER"))
                .andExpect(jsonPath("$.data.email").value("alice.e2e@test.com"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/token").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Full flow: register EMPLOYER → receive JWT → verify employer role")
    void registerEmployer_fullFlow_returnsEmployerRole() throws Exception {
        RegisterRequest req = buildRegisterRequest(
                "ACME Corp", "acme.e2e@test.com", "password123", User.Role.EMPLOYER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("EMPLOYER"))
                .andExpect(jsonPath("$.data.fullName").value("ACME Corp"));
    }

    @Test
    @DisplayName("Full flow: register → login → receive fresh JWT")
    void registerThenLogin_fullFlow_returnsJwtOnLogin() throws Exception {
        // Step 1: register
        RegisterRequest reg = buildRegisterRequest(
                "Bob Seeker", "bob.e2e@test.com", "password123", User.Role.JOB_SEEKER);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Step 2: login with same credentials
        LoginRequest login = new LoginRequest();
        login.setEmail("bob.e2e@test.com");
        login.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("bob.e2e@test.com"));
    }

    @Test
    @DisplayName("Register with duplicate email → 400 Bad Request")
    void register_duplicateEmail_returns400() throws Exception {
        RegisterRequest req = buildRegisterRequest(
                "Dup User", "dup.e2e@test.com", "password123", User.Role.JOB_SEEKER);

        // First registration — should succeed
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second registration with same email — should fail
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    @DisplayName("Login with wrong password → 401 Unauthorized")
    void login_wrongPassword_returns401() throws Exception {
        // Register first
        RegisterRequest reg = buildRegisterRequest(
                "Carol", "carol.e2e@test.com", "correct123", User.Role.JOB_SEEKER);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Login with wrong password
        LoginRequest login = new LoginRequest();
        login.setEmail("carol.e2e@test.com");
        login.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Register with invalid email format → 400 Bad Request")
    void register_invalidEmailFormat_returns400() throws Exception {
        RegisterRequest req = buildRegisterRequest(
                "Test", "not-an-email", "password123", User.Role.JOB_SEEKER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Register with short password → 400 Bad Request")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = buildRegisterRequest(
                "Test", "test.pw@test.com", "123", User.Role.JOB_SEEKER);  // < 8 chars

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").isNotEmpty());
    }

    // ── Helpers ──────────────────────────────────────────────────

    private RegisterRequest buildRegisterRequest(String name, String email,
                                                  String password, User.Role role) {
        RegisterRequest req = new RegisterRequest();
        req.setFullName(name);
        req.setEmail(email);
        req.setPassword(password);
        req.setRole(role);
        return req;
    }
}
