package com.jobconnect.controller;

import com.jobconnect.dto.request.JobApplicationRequest;
import com.jobconnect.dto.response.ApiResponse;
import com.jobconnect.dto.response.JobApplicationResponse;
import com.jobconnect.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seeker")
@RequiredArgsConstructor
@Tag(name = "Job Seeker", description = "Job applications for seekers (JOB_SEEKER role)")
public class SeekerController {

    private final ApplicationService applicationService;

    @PostMapping("/jobs/{jobId}/apply")
    @Operation(summary = "Apply to a job")
    public ResponseEntity<ApiResponse<JobApplicationResponse>> apply(
            @PathVariable Long jobId,
            @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        JobApplicationResponse response = applicationService.applyToJob(
                jobId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully", response));
    }

    @GetMapping("/applications")
    @Operation(summary = "View all applications submitted by the authenticated seeker")
    public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> myApplications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Your applications",
                applicationService.getMyApplications(userDetails.getUsername())));
    }
}
