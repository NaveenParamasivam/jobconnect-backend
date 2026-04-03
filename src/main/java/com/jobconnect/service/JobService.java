package com.jobconnect.service;

import com.jobconnect.dto.request.JobRequest;
import com.jobconnect.dto.response.JobResponse;

import java.util.List;

public interface JobService {
    JobResponse createJob(JobRequest request, String employerEmail);
    JobResponse updateJob(Long jobId, JobRequest request, String employerEmail);
    void deleteJob(Long jobId, String employerEmail);
    JobResponse getJobById(Long jobId);
    List<JobResponse> getAllActiveJobs();
    List<JobResponse> searchJobs(String keyword, String location, String category, String jobType);
    List<JobResponse> getJobsByEmployer(String employerEmail);
}
