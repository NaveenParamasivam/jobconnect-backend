package com.jobconnect.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobconnect.dto.request.JobRequest;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for the full job management workflow:
 *   Employer registers → posts job → Seeker registers → browses →
 *   applies → Employer updates status.
 *
 * Full Spring context + H2. No mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("E2E — Job Workflow")
class JobWorkflowE2ETest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String employerToken;
    private String seekerToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        employerToken = registerAndGetToken(
                "ACME Corp", "acme.job@test.com", "password123", User.Role.EMPLOYER);
        seekerToken = registerAndGetToken(
                "Alice Dev", "alice.job@test.com", "password123", User.Role.JOB_SEEKER);
    }

    // ── Employer: Job CRUD ────────────────────────────────────────

    @Test
    @DisplayName("Employer posts a job → job appears in public listing")
    void employerPostsJob_appearsInPublicList() throws Exception {
        // Create job
        long jobId = createJob(buildJobRequest("Backend Developer", "Chennai"));

        // Verify in public listing
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Backend Developer"))
                .andExpect(jsonPath("$.data[0].location").value("Chennai"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Employer posts job → updates it → changes reflected")
    void employerUpdatesJob_changesAreReflected() throws Exception {
        long jobId = createJob(buildJobRequest("Old Title", "Mumbai"));

        JobRequest updated = buildJobRequest("New Title", "Bangalore");
        mockMvc.perform(put("/api/employer/jobs/" + jobId)
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New Title"))
                .andExpect(jsonPath("$.data.location").value("Bangalore"));
    }

    @Test
    @DisplayName("Employer deletes job → job no longer in listing")
    void employerDeletesJob_removedFromListing() throws Exception {
        long jobId = createJob(buildJobRequest("Temp Job", "Delhi"));

        // Verify it exists
        mockMvc.perform(get("/api/jobs")).andExpect(jsonPath("$.data").isArray());

        // Delete it
        mockMvc.perform(delete("/api/employer/jobs/" + jobId)
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job deleted successfully"));

        // Verify removed
        mockMvc.perform(get("/api/jobs"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Seeker cannot create a job → 403 Forbidden")
    void seeker_cannotCreateJob_returns403() throws Exception {
        mockMvc.perform(post("/api/employer/jobs")
                        .header("Authorization", "Bearer " + seekerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildJobRequest("Test", "City"))))
                .andExpect(status().isForbidden());
    }

    // ── Public Job Search ─────────────────────────────────────────

    @Test
    @DisplayName("Seeker searches jobs by keyword → matching jobs returned")
    void searchByKeyword_returnsMatchingJobs() throws Exception {
        createJob(buildJobRequest("Backend Developer", "Chennai"));
        createJob(buildJobRequest("Frontend Designer", "Bangalore"));

        mockMvc.perform(get("/api/jobs/search").param("keyword", "Backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Backend Developer"));
    }

    @Test
    @DisplayName("Seeker searches jobs by location → matching jobs returned")
    void searchByLocation_returnsMatchingJobs() throws Exception {
        createJob(buildJobRequest("Dev Role A", "Chennai"));
        createJob(buildJobRequest("Dev Role B", "Bangalore"));

        mockMvc.perform(get("/api/jobs/search").param("location", "Chennai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].location").value("Chennai"));
    }

    @Test
    @DisplayName("Unauthenticated user can browse jobs without token")
    void browseJobs_noToken_returns200() throws Exception {
        createJob(buildJobRequest("Public Job", "Remote"));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Seeker: Apply ─────────────────────────────────────────────

    @Test
    @DisplayName("Full workflow: Employer posts → Seeker applies → Employer sees application")
    void fullWorkflow_employerPostsJob_seekerApplies_employerSeesApplication() throws Exception {
        // Step 1: employer creates job
        long jobId = createJob(buildJobRequest("Full Stack Developer", "Chennai"));

        // Step 2: seeker applies
        String applyBody = """
                {"coverLetter": "I have 5 years of experience in full-stack development."}
                """;

        mockMvc.perform(post("/api/seeker/jobs/" + jobId + "/apply")
                        .header("Authorization", "Bearer " + seekerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.jobTitle").value("Full Stack Developer"));

        // Step 3: employer sees application
        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].applicantEmail").value("alice.job@test.com"));
    }

    @Test
    @DisplayName("Seeker applies twice to same job → 400 Bad Request")
    void seeker_appliesTwice_returns400() throws Exception {
        long jobId = createJob(buildJobRequest("DevOps Engineer", "Pune"));
        String body = """
                {"coverLetter": "First application."}
                """;

        // First application — should succeed
        mockMvc.perform(post("/api/seeker/jobs/" + jobId + "/apply")
                        .header("Authorization", "Bearer " + seekerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Second application — should fail
        mockMvc.perform(post("/api/seeker/jobs/" + jobId + "/apply")
                        .header("Authorization", "Bearer " + seekerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You have already applied to this job"));
    }

    @Test
    @DisplayName("Employer cannot apply to jobs → 403 Forbidden")
    void employer_cannotApplyToJob_returns403() throws Exception {
        long jobId = createJob(buildJobRequest("Data Scientist", "Mumbai"));

        mockMvc.perform(post("/api/seeker/jobs/" + jobId + "/apply")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ── Employer: Update Application Status ───────────────────────

    @Test
    @DisplayName("Full workflow: apply → employer shortlists → seeker sees updated status")
    void fullWorkflow_employerShortlistsApplicant_seekerSeesUpdate() throws Exception {
        // Setup
        long jobId = createJob(buildJobRequest("QA Engineer", "Hyderabad"));
        mockMvc.perform(post("/api/seeker/jobs/" + jobId + "/apply")
                .header("Authorization", "Bearer " + seekerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coverLetter\": \"Great QA background.\"}"));

        // Employer gets applications and extracts applicationId
        MvcResult appsResult = mockMvc.perform(
                get("/api/employer/jobs/" + jobId + "/applications")
                        .header("Authorization", "Bearer " + employerToken))
                .andReturn();
        long appId = objectMapper.readTree(appsResult.getResponse().getContentAsString())
                .at("/data/0/id").asLong();

        // Employer shortlists the applicant
        mockMvc.perform(patch("/api/employer/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SHORTLISTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));

        // Seeker sees updated status
        mockMvc.perform(get("/api/seeker/applications")
                        .header("Authorization", "Bearer " + seekerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SHORTLISTED"));
    }

    @Test
    @DisplayName("Wrong employer cannot update another employer's application status")
    void wrongEmployer_cannotUpdateApplicationStatus_returns403() throws Exception {
        long jobId = createJob(buildJobRequest("PM Role", "Gurgaon"));
        mockMvc.perform(post("/api/seeker/jobs/" + jobId + "/apply")
                .header("Authorization", "Bearer " + seekerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coverLetter\": \"PM experience.\"}"));

        // Get application id
        MvcResult appsResult = mockMvc.perform(
                get("/api/employer/jobs/" + jobId + "/applications")
                        .header("Authorization", "Bearer " + employerToken))
                .andReturn();
        long appId = objectMapper.readTree(appsResult.getResponse().getContentAsString())
                .at("/data/0/id").asLong();

        // Register a second employer
        String otherToken = registerAndGetToken(
                "Other Corp", "other.employer@test.com", "password123", User.Role.EMPLOYER);

        // Other employer tries to update — should be forbidden
        mockMvc.perform(patch("/api/employer/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String registerAndGetToken(String name, String email, String password,
                                        User.Role role) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName(name); req.setEmail(email);
        req.setPassword(password); req.setRole(role);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/token").asText();
    }

    private long createJob(JobRequest req) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/employer/jobs")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private JobRequest buildJobRequest(String title, String location) {
        JobRequest req = new JobRequest();
        req.setTitle(title);
        req.setDescription("A great opportunity for skilled professionals.");
        req.setLocation(location);
        req.setSalary(75000.0);
        req.setDeadline(LocalDate.now().plusMonths(2));
        req.setJobType("FULL_TIME");
        req.setCategory("Engineering");
        return req;
    }
}
