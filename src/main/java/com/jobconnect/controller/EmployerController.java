package com.jobconnect.controller;

import com.jobconnect.dto.request.JobRequest;
import com.jobconnect.dto.request.UpdateApplicationStatusRequest;
import com.jobconnect.dto.response.ApiResponse;
import com.jobconnect.dto.response.JobApplicationResponse;
import com.jobconnect.dto.response.JobResponse;
import com.jobconnect.service.ApplicationService;
import com.jobconnect.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
@Tag(name = "Employer", description = "Job posting and application management (EMPLOYER role)")
public class EmployerController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    // ── Job Management ───────────────────────────────────────────

    @PostMapping("/jobs")
    @Operation(summary = "Create a new job posting")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        JobResponse job = jobService.createJob(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job created successfully", job));
    }

    @PutMapping("/jobs/{jobId}")
    @Operation(summary = "Update an existing job posting")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable Long jobId,
            @Valid @RequestBody JobRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        JobResponse job = jobService.updateJob(jobId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Job updated successfully", job));
    }

    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Delete a job posting")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        jobService.deleteJob(jobId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Job deleted successfully"));
    }

    @GetMapping("/jobs")
    @Operation(summary = "Get all jobs posted by the authenticated employer")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getMyJobs(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Your jobs",
                jobService.getJobsByEmployer(userDetails.getUsername())));
    }

    // ── Application Management ───────────────────────────────────

    @GetMapping("/applications")
    @Operation(summary = "Get all applications across all of the employer's jobs")
    public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> getAllApplications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Applications fetched",
                applicationService.getApplicationsForEmployer(userDetails.getUsername())));
    }

    @GetMapping("/jobs/{jobId}/applications")
    @Operation(summary = "Get all applications for a specific job")
    public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> getJobApplications(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Applications fetched",
                applicationService.getApplicationsForJob(jobId, userDetails.getUsername())));
    }

    @PatchMapping("/applications/{applicationId}/status")
    @Operation(summary = "Update application status (REVIEWED / SHORTLISTED / REJECTED / ACCEPTED)")
    public ResponseEntity<ApiResponse<JobApplicationResponse>> updateStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        JobApplicationResponse updated = applicationService.updateApplicationStatus(
                applicationId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Status updated", updated));
    }
}
