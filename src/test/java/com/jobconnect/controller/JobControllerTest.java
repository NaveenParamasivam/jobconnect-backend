package com.jobconnect.controller;

import com.jobconnect.config.SecurityConfig;
import com.jobconnect.dto.response.JobResponse;
import com.jobconnect.entity.Job;
import com.jobconnect.service.JobService;
import com.jobconnect.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = JobController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@DisplayName("JobController — Integration Tests")
@Import(SecurityConfig.class)
class JobControllerTest {

    @Autowired MockMvc    mockMvc;

    @MockBean JobService         jobService;
    @MockBean JwtUtil            jwtUtil;
    @MockBean UserDetailsService userDetailsService;

    private JobResponse sampleJob;

    @BeforeEach
    void setUp() {
        sampleJob = JobResponse.builder()
                .id(1L)
                .title("Backend Developer")
                .description("Spring Boot dev")
                .location("Chennai")
                .salary(80000.0)
                .deadline(LocalDate.now().plusMonths(1))
                .jobType("FULL_TIME")
                .category("Engineering")
                .status(Job.JobStatus.ACTIVE)
                .employerId(10L)
                .employerName("ACME Corp")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/jobs — returns list of active jobs (public)")
    void getAllJobs_public_returns200() throws Exception {
        when(jobService.getAllActiveJobs()).thenReturn(List.of(sampleJob));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Backend Developer"))
                .andExpect(jsonPath("$.data[0].location").value("Chennai"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/jobs/{id} — returns single job by id")
    void getJobById_exists_returns200() throws Exception {
        when(jobService.getJobById(1L)).thenReturn(sampleJob);

        mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.employerName").value("ACME Corp"));
    }

    @Test
    @DisplayName("GET /api/jobs/search — filters by keyword and location")
    void searchJobs_withParams_returns200() throws Exception {
        when(jobService.searchJobs("spring", "Chennai", null, null))
                .thenReturn(List.of(sampleJob));

        mockMvc.perform(get("/api/jobs/search")
                        .param("keyword", "spring")
                        .param("location", "Chennai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Backend Developer"));
    }
}