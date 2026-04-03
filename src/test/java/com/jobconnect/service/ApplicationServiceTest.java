package com.jobconnect.service;

import com.jobconnect.dto.request.JobApplicationRequest;
import com.jobconnect.dto.request.UpdateApplicationStatusRequest;
import com.jobconnect.dto.response.JobApplicationResponse;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import com.jobconnect.exception.BadRequestException;
import com.jobconnect.exception.UnauthorizedException;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationService — Unit Tests")
class ApplicationServiceTest {

    @Mock JobApplicationRepository applicationRepository;
    @Mock JobRepository            jobRepository;
    @Mock UserRepository           userRepository;
    @Mock SmsService               smsService;

    @InjectMocks ApplicationServiceImpl applicationService;

    private User seeker;
    private User employer;
    private Job  activeJob;
    private JobApplication pendingApp;

    @BeforeEach
    void setUp() {
        seeker = User.builder()
                .id(2L).fullName("Alice").email("alice@test.com")
                .role(User.Role.JOB_SEEKER).phoneNumber("+919999999999").build();

        employer = User.builder()
                .id(1L).fullName("ACME Corp").email("acme@corp.com")
                .role(User.Role.EMPLOYER).build();

        activeJob = Job.builder()
                .id(10L).title("Backend Developer").description("Java dev")
                .location("Chennai").deadline(LocalDate.now().plusMonths(1))
                .status(Job.JobStatus.ACTIVE).employer(employer)
                .createdAt(LocalDateTime.now()).build();

        pendingApp = JobApplication.builder()
                .id(100L).job(activeJob).applicant(seeker)
                .status(JobApplication.ApplicationStatus.PENDING)
                .appliedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("applyToJob() — seeker applies successfully")
    void applyToJob_validRequest_returnsResponse() {
        JobApplicationRequest req = new JobApplicationRequest();
        req.setCoverLetter("I am a great fit!");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(seeker));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(activeJob));
        when(applicationRepository.existsByJobIdAndApplicantId(10L, 2L)).thenReturn(false);
        when(applicationRepository.save(any())).thenReturn(pendingApp);

        JobApplicationResponse result = applicationService.applyToJob(10L, req, "alice@test.com");

        assertThat(result.getStatus()).isEqualTo(JobApplication.ApplicationStatus.PENDING);
        assertThat(result.getJobTitle()).isEqualTo("Backend Developer");
        verify(applicationRepository).save(any(JobApplication.class));
    }

    @Test
    @DisplayName("applyToJob() — duplicate application throws BadRequestException")
    void applyToJob_duplicate_throwsBadRequest() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(seeker));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(activeJob));
        when(applicationRepository.existsByJobIdAndApplicantId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.applyToJob(
                        10L, new JobApplicationRequest(), "alice@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already applied");
    }

    @Test
    @DisplayName("applyToJob() — closed job throws BadRequestException")
    void applyToJob_closedJob_throwsBadRequest() {
        activeJob.setStatus(Job.JobStatus.CLOSED);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(seeker));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(activeJob));

        assertThatThrownBy(() -> applicationService.applyToJob(
                        10L, new JobApplicationRequest(), "alice@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer accepting");
    }

    @Test
    @DisplayName("updateApplicationStatus() — employer updates to ACCEPTED")
    void updateStatus_validEmployer_updatesStatus() {
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus(JobApplication.ApplicationStatus.ACCEPTED);

        pendingApp.setStatus(JobApplication.ApplicationStatus.ACCEPTED);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(pendingApp));
        when(applicationRepository.save(any())).thenReturn(pendingApp);

        JobApplicationResponse result = applicationService.updateApplicationStatus(
                100L, req, "acme@corp.com");

        assertThat(result.getStatus()).isEqualTo(JobApplication.ApplicationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("updateApplicationStatus() — wrong employer throws UnauthorizedException")
    void updateStatus_wrongEmployer_throwsUnauthorized() {
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus(JobApplication.ApplicationStatus.REJECTED);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(pendingApp));

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(
                        100L, req, "stranger@corp.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getMyApplications() — returns all seeker applications")
    void getMyApplications_returnsCorrectList() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(seeker));
        when(applicationRepository.findByApplicantId(2L)).thenReturn(List.of(pendingApp));

        List<JobApplicationResponse> results =
                applicationService.getMyApplications("alice@test.com");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getApplicantEmail()).isEqualTo("alice@test.com");
    }
}
