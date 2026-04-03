package com.jobconnect.service;

import com.jobconnect.dto.request.JobApplicationRequest;
import com.jobconnect.dto.request.UpdateApplicationStatusRequest;
import com.jobconnect.dto.response.JobApplicationResponse;

import java.util.List;

public interface ApplicationService {
    JobApplicationResponse applyToJob(Long jobId, JobApplicationRequest request, String seekerEmail);
    List<JobApplicationResponse> getMyApplications(String seekerEmail);
    List<JobApplicationResponse> getApplicationsForEmployer(String employerEmail);
    List<JobApplicationResponse> getApplicationsForJob(Long jobId, String employerEmail);
    JobApplicationResponse updateApplicationStatus(Long applicationId,
            UpdateApplicationStatusRequest request, String employerEmail);
}
