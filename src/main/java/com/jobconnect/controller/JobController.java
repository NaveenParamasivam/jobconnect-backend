package com.jobconnect.controller;

import com.jobconnect.dto.response.ApiResponse;
import com.jobconnect.dto.response.JobResponse;
import com.jobconnect.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs (Public)", description = "Browse and search job listings — no auth required")
public class JobController {

    private final JobService jobService;

    @GetMapping
    @Operation(summary = "List all active job postings")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getAllJobs() {
        return ResponseEntity.ok(
                ApiResponse.success("Jobs fetched", jobService.getAllActiveJobs()));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get a single job by ID")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(
                ApiResponse.success("Job fetched", jobService.getJobById(jobId)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search jobs by keyword, location, category, and/or jobType")
    public ResponseEntity<ApiResponse<List<JobResponse>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String jobType) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
                jobService.searchJobs(keyword, location, category, jobType)));
    }
}
