package com.jobconnect.service.impl;

import com.jobconnect.dto.request.JobApplicationRequest;
import com.jobconnect.dto.request.UpdateApplicationStatusRequest;
import com.jobconnect.dto.response.JobApplicationResponse;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import com.jobconnect.exception.BadRequestException;
import com.jobconnect.exception.ResourceNotFoundException;
import com.jobconnect.exception.UnauthorizedException;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.service.ApplicationService;
import com.jobconnect.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;

    @Override
    @Transactional
    public JobApplicationResponse applyToJob(Long jobId, JobApplicationRequest request,
                                              String seekerEmail) {
        User seeker = findUserByEmail(seekerEmail);
        Job job = findJobById(jobId);

        if (job.getStatus() != Job.JobStatus.ACTIVE) {
            throw new BadRequestException("This job is no longer accepting applications");
        }
        if (applicationRepository.existsByJobIdAndApplicantId(jobId, seeker.getId())) {
            throw new BadRequestException("You have already applied to this job");
        }

        JobApplication application = JobApplication.builder()
                .job(job)
                .applicant(seeker)
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .build();

        JobApplication saved = applicationRepository.save(application);
        log.info("User [{}] applied to job [id={}]", seekerEmail, jobId);

        // SMS confirmation to seeker
        if (StringUtils.hasText(seeker.getPhoneNumber())) {
            try {
                smsService.sendApplicationConfirmation(
                        seeker.getPhoneNumber(), seeker.getFullName(), job.getTitle());
            } catch (Exception ex) {
                log.warn("SMS failed for seeker [{}]: {}", seekerEmail, ex.getMessage());
            }
        }

        return JobApplicationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getMyApplications(String seekerEmail) {
        User seeker = findUserByEmail(seekerEmail);
        return applicationRepository.findByApplicantId(seeker.getId())
                .stream().map(JobApplicationResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplicationsForEmployer(String employerEmail) {
        User employer = findUserByEmail(employerEmail);
        return applicationRepository.findByJobEmployerId(employer.getId())
                .stream().map(JobApplicationResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplicationsForJob(Long jobId, String employerEmail) {
        Job job = findJobById(jobId);
        if (!job.getEmployer().getEmail().equals(employerEmail)) {
            throw new UnauthorizedException("You are not authorised to view these applications");
        }
        return applicationRepository.findByJobId(jobId)
                .stream().map(JobApplicationResponse::fromEntity).toList();
    }

    @Override
    @Transactional
    public JobApplicationResponse updateApplicationStatus(Long applicationId,
            UpdateApplicationStatusRequest request, String employerEmail) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));

        if (!application.getJob().getEmployer().getEmail().equals(employerEmail)) {
            throw new UnauthorizedException("You are not authorised to update this application");
        }

        application.setStatus(request.getStatus());
        JobApplication updated = applicationRepository.save(application);
        log.info("Application [id={}] status changed to [{}] by [{}]",
                applicationId, request.getStatus(), employerEmail);

        // SMS status update to seeker
        User seeker = updated.getApplicant();
        if (StringUtils.hasText(seeker.getPhoneNumber())) {
            try {
                smsService.sendApplicationStatusUpdate(
                        seeker.getPhoneNumber(),
                        seeker.getFullName(),
                        updated.getJob().getTitle(),
                        request.getStatus().name());
            } catch (Exception ex) {
                log.warn("SMS status update failed: {}", ex.getMessage());
            }
        }

        return JobApplicationResponse.fromEntity(updated);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Job findJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }
}
