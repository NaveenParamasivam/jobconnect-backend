package com.jobconnect.service;

import com.jobconnect.dto.request.JobRequest;
import com.jobconnect.dto.response.JobResponse;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import com.jobconnect.exception.ResourceNotFoundException;
import com.jobconnect.exception.UnauthorizedException;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.service.impl.JobServiceImpl;
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
@DisplayName("JobService — Unit Tests")
class JobServiceTest {

    @Mock JobRepository  jobRepository;
    @Mock UserRepository userRepository;
    @Mock SmsService     smsService;

    @InjectMocks JobServiceImpl jobService;

    private User employer;
    private Job  activeJob;
    private JobRequest jobRequest;

    @BeforeEach
    void setUp() {
        employer = User.builder()
                .id(1L).fullName("ACME Corp").email("acme@corp.com")
                .role(User.Role.EMPLOYER).build();

        activeJob = Job.builder()
                .id(10L).title("Backend Developer").description("Spring Boot dev")
                .location("Chennai").salary(80000.0)
                .deadline(LocalDate.now().plusMonths(1))
                .jobType("FULL_TIME").category("Engineering")
                .status(Job.JobStatus.ACTIVE).employer(employer)
                .createdAt(LocalDateTime.now())
                .build();

        jobRequest = new JobRequest();
        jobRequest.setTitle("Backend Developer");
        jobRequest.setDescription("Spring Boot dev");
        jobRequest.setLocation("Chennai");
        jobRequest.setSalary(80000.0);
        jobRequest.setDeadline(LocalDate.now().plusMonths(1));
        jobRequest.setJobType("FULL_TIME");
        jobRequest.setCategory("Engineering");
    }

    @Test
    @DisplayName("createJob() — employer creates job successfully")
    void createJob_validEmployer_returnsJobResponse() {
        when(userRepository.findByEmail("acme@corp.com")).thenReturn(Optional.of(employer));
        when(jobRepository.save(any(Job.class))).thenReturn(activeJob);

        JobResponse result = jobService.createJob(jobRequest, "acme@corp.com");

        assertThat(result.getTitle()).isEqualTo("Backend Developer");
        assertThat(result.getEmployerName()).isEqualTo("ACME Corp");
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    @DisplayName("createJob() — non-existent employer throws ResourceNotFoundException")
    void createJob_unknownEmployer_throwsNotFound() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.createJob(jobRequest, "ghost@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateJob() — wrong employer throws UnauthorizedException")
    void updateJob_wrongEmployer_throwsUnauthorized() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(activeJob));

        assertThatThrownBy(() -> jobService.updateJob(10L, jobRequest, "other@corp.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("deleteJob() — owner can delete their job")
    void deleteJob_owner_succeeds() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(activeJob));
        doNothing().when(jobRepository).delete(activeJob);

        assertThatCode(() -> jobService.deleteJob(10L, "acme@corp.com"))
                .doesNotThrowAnyException();

        verify(jobRepository).delete(activeJob);
    }

    @Test
    @DisplayName("getJobById() — job not found throws ResourceNotFoundException")
    void getJobById_notFound_throws() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getAllActiveJobs() — returns only ACTIVE jobs")
    void getAllActiveJobs_returnsActiveOnly() {
        when(jobRepository.findByStatus(Job.JobStatus.ACTIVE)).thenReturn(List.of(activeJob));

        List<JobResponse> results = jobService.getAllActiveJobs();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(Job.JobStatus.ACTIVE);
    }

    @Test
    @DisplayName("searchJobs() — delegates to repository with correct params")
    void searchJobs_delegatesCorrectly() {
        when(jobRepository.search("spring", "Chennai", null, null))
                .thenReturn(List.of(activeJob));

        List<JobResponse> results = jobService.searchJobs("spring", "Chennai", null, null);

        assertThat(results).hasSize(1);
        verify(jobRepository).search("spring", "Chennai", null, null);
    }
}
