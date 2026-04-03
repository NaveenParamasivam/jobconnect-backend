package com.jobconnect.service.impl;

import com.jobconnect.dto.request.JobRequest;
import com.jobconnect.dto.response.JobResponse;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import com.jobconnect.exception.BadRequestException;
import com.jobconnect.exception.ResourceNotFoundException;
import com.jobconnect.exception.UnauthorizedException;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.service.JobService;
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
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;

    @Override
    @Transactional
    public JobResponse createJob(JobRequest request, String employerEmail) {
        User employer = findUserByEmail(employerEmail);
        assertEmployer(employer);

        Job job = Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .salary(request.getSalary())
                .deadline(request.getDeadline())
                .jobType(request.getJobType())
                .category(request.getCategory())
                .employer(employer)
                .build();

        Job saved = jobRepository.save(job);
        log.info("Job created [id={}] by employer [{}]", saved.getId(), employerEmail);

        // Notify employer via SMS
        if (StringUtils.hasText(employer.getPhoneNumber())) {
            try {
                smsService.sendJobPostingAlert(employer.getPhoneNumber(),
                        employer.getFullName(), saved.getTitle());
            } catch (Exception ex) {
                log.warn("SMS alert failed: {}", ex.getMessage());
            }
        }

        return JobResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public JobResponse updateJob(Long jobId, JobRequest request, String employerEmail) {
        Job job = findJobById(jobId);
        assertOwnership(job, employerEmail);

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setLocation(request.getLocation());
        job.setSalary(request.getSalary());
        job.setDeadline(request.getDeadline());
        job.setJobType(request.getJobType());
        job.setCategory(request.getCategory());

        return JobResponse.fromEntity(jobRepository.save(job));
    }

    @Override
    @Transactional
    public void deleteJob(Long jobId, String employerEmail) {
        Job job = findJobById(jobId);
        assertOwnership(job, employerEmail);
        jobRepository.delete(job);
        log.info("Job [id={}] deleted by employer [{}]", jobId, employerEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long jobId) {
        return JobResponse.fromEntity(findJobById(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllActiveJobs() {
        return jobRepository.findByStatus(Job.JobStatus.ACTIVE)
                .stream().map(JobResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> searchJobs(String keyword, String location,
                                        String category, String jobType) {
        return jobRepository.search(
                blankToNull(keyword),
                blankToNull(location),
                blankToNull(category),
                blankToNull(jobType)
        ).stream().map(JobResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByEmployer(String employerEmail) {
        User employer = findUserByEmail(employerEmail);
        return jobRepository.findByEmployerId(employer.getId())
                .stream().map(JobResponse::fromEntity).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private Job findJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void assertEmployer(User user) {
        if (user.getRole() != User.Role.EMPLOYER) {
            throw new BadRequestException("Only employers can manage job postings");
        }
    }

    private void assertOwnership(Job job, String employerEmail) {
        if (!job.getEmployer().getEmail().equals(employerEmail)) {
            throw new UnauthorizedException("You are not authorised to modify this job");
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
