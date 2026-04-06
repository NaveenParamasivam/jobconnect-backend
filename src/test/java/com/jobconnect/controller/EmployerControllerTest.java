package com.jobconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobconnect.dto.request.JobRequest;
import com.jobconnect.dto.request.UpdateApplicationStatusRequest;
import com.jobconnect.dto.response.JobApplicationResponse;
import com.jobconnect.dto.response.JobResponse;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.service.ApplicationService;
import com.jobconnect.service.JobService;
import com.jobconnect.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EmployerController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@DisplayName("EmployerController — Integration Tests")
class EmployerControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @MockBean  JobService    jobService;
    @MockBean  ApplicationService applicationService;
    @MockBean  JwtUtil       jwtUtil;
    @MockBean  UserDetailsService userDetailsService;

    private JobResponse           sampleJob;
    private JobApplicationResponse sampleApp;
    private JobRequest            validJobRequest;

    @BeforeEach
    void setUp() {
        sampleJob = JobResponse.builder()
                .id(1L).title("Backend Developer").description("Java dev")
                .location("Chennai").salary(80000.0)
                .deadline(LocalDate.now().plusMonths(1))
                .jobType("FULL_TIME").category("Engineering")
                .status(Job.JobStatus.ACTIVE)
                .employerId(10L).employerName("ACME Corp")
                .createdAt(LocalDateTime.now()).build();

        sampleApp = JobApplicationResponse.builder()
                .id(100L).jobId(1L).jobTitle("Backend Developer")
                .applicantId(2L).applicantName("Alice").applicantEmail("alice@test.com")
                .status(JobApplication.ApplicationStatus.PENDING)
                .appliedAt(LocalDateTime.now()).build();

        validJobRequest = new JobRequest();
        validJobRequest.setTitle("Backend Developer");
        validJobRequest.setDescription("Java Spring Boot development role");
        validJobRequest.setLocation("Chennai");
        validJobRequest.setSalary(80000.0);
        validJobRequest.setDeadline(LocalDate.now().plusMonths(1));
        validJobRequest.setJobType("FULL_TIME");
        validJobRequest.setCategory("Engineering");
    }

    @Test
    @WithMockUser(username = "acme@corp.com", roles = "EMPLOYER")
    @DisplayName("POST /api/employer/jobs — 201 Created on valid job request")
    void createJob_validRequest_returns201() throws Exception {
        when(jobService.createJob(any(), eq("acme@corp.com"))).thenReturn(sampleJob);

        mockMvc.perform(post("/api/employer/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Backend Developer"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "acme@corp.com", roles = "EMPLOYER")
    @DisplayName("POST /api/employer/jobs — 400 Bad Request on missing required fields")
    void createJob_missingTitle_returns400() throws Exception {
        validJobRequest.setTitle("");

        mockMvc.perform(post("/api/employer/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "acme@corp.com", roles = "EMPLOYER")
    @DisplayName("PUT /api/employer/jobs/{id} — 200 OK on valid update")
    void updateJob_validRequest_returns200() throws Exception {
        when(jobService.updateJob(eq(1L), any(), eq("acme@corp.com"))).thenReturn(sampleJob);

        mockMvc.perform(put("/api/employer/jobs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "acme@corp.com", roles = "EMPLOYER")
    @DisplayName("DELETE /api/employer/jobs/{id} — 200 OK on successful delete")
    void deleteJob_existing_returns200() throws Exception {
        doNothing().when(jobService).deleteJob(eq(1L), eq("acme@corp.com"));

        mockMvc.perform(delete("/api/employer/jobs/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Job deleted successfully"));
    }

    @Test
    @WithMockUser(username = "acme@corp.com", roles = "EMPLOYER")
    @DisplayName("GET /api/employer/jobs — returns employer's posted jobs")
    void getMyJobs_returnsJobList() throws Exception {
        when(jobService.getJobsByEmployer("acme@corp.com")).thenReturn(List.of(sampleJob));

        mockMvc.perform(get("/api/employer/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Backend Developer"));
    }

    @Test
    @DisplayName("POST /api/employer/jobs — 403 Forbidden without authentication")
    void createJob_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/employer/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "acme@corp.com", roles = "EMPLOYER")
    @DisplayName("GET /api/employer/applications — returns all employer applications")
    void getAllApplications_returnsApplicationList() throws Exception {
        when(applicationService.getApplicationsForEmployer("acme@corp.com"))
                .thenReturn(List.of(sampleApp));

        mockMvc.perform(get("/api/employer/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].applicantName").value("Alice"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "acme@corp.com", roles = "EMPLOYER")
    @DisplayName("GET /api/employer/jobs/{jobId}/applications — returns job-specific applications")
    void getJobApplications_returnsCorrectList() throws Exception {
        when(applicationService.getApplicationsForJob(1L, "acme@corp.com"))
                .thenReturn(List.of(sampleApp));

        mockMvc.perform(get("/api/employer/jobs/1/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobId").value(1));
    }

    @Test
    @WithMockUser(username = "acme@corp.com", roles = "EMPLOYER")
    @DisplayName("PATCH /api/employer/applications/{id}/status — 200 OK on valid status update")
    void updateApplicationStatus_valid_returns200() throws Exception {
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus(JobApplication.ApplicationStatus.SHORTLISTED);

        JobApplicationResponse updated = JobApplicationResponse.builder()
                .id(100L).status(JobApplication.ApplicationStatus.SHORTLISTED)
                .jobId(1L).jobTitle("Backend Developer")
                .applicantId(2L).applicantName("Alice").applicantEmail("alice@test.com")
                .appliedAt(LocalDateTime.now()).build();

        when(applicationService.updateApplicationStatus(eq(100L), any(), eq("acme@corp.com")))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/employer/applications/100/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));
    }
}