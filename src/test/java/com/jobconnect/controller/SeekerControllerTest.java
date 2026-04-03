package com.jobconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobconnect.dto.request.JobApplicationRequest;
import com.jobconnect.dto.response.JobApplicationResponse;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.service.ApplicationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeekerController.class)
@DisplayName("SeekerController — Integration Tests")
class SeekerControllerTest {

    @Autowired MockMvc            mockMvc;
    @Autowired ObjectMapper       objectMapper;
    @MockBean  ApplicationService applicationService;

    private JobApplicationResponse sampleApp;

    @BeforeEach
    void setUp() {
        sampleApp = JobApplicationResponse.builder()
                .id(1L).jobId(10L).jobTitle("Backend Developer").jobLocation("Chennai")
                .applicantId(2L).applicantName("Alice").applicantEmail("alice@test.com")
                .coverLetter("I am a great fit!").status(JobApplication.ApplicationStatus.PENDING)
                .appliedAt(LocalDateTime.now()).build();
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "JOB_SEEKER")
    @DisplayName("POST /api/seeker/jobs/{jobId}/apply — 201 Created on valid application")
    void apply_validRequest_returns201() throws Exception {
        JobApplicationRequest req = new JobApplicationRequest();
        req.setCoverLetter("I am a great fit!");

        when(applicationService.applyToJob(eq(10L), any(), eq("alice@test.com")))
                .thenReturn(sampleApp);

        mockMvc.perform(post("/api/seeker/jobs/10/apply")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobTitle").value("Backend Developer"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/seeker/jobs/{jobId}/apply — 403 Forbidden without authentication")
    void apply_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/seeker/jobs/10/apply")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "JOB_SEEKER")
    @DisplayName("GET /api/seeker/applications — returns seeker's own applications")
    void getMyApplications_returnsApplicationList() throws Exception {
        when(applicationService.getMyApplications("alice@test.com"))
                .thenReturn(List.of(sampleApp));

        mockMvc.perform(get("/api/seeker/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].applicantEmail").value("alice@test.com"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/seeker/applications — 403 Forbidden without authentication")
    void getMyApplications_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/seeker/applications"))
                .andExpect(status().isForbidden());
    }
}
