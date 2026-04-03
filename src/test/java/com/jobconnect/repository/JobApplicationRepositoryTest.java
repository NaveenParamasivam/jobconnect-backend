package com.jobconnect.repository;

import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("JobApplicationRepository — Integration Tests")
class JobApplicationRepositoryTest {

    @Autowired JobApplicationRepository applicationRepository;
    @Autowired JobRepository            jobRepository;
    @Autowired UserRepository           userRepository;

    private User employer;
    private User seeker1;
    private User seeker2;
    private Job  job;

    @BeforeEach
    void setUp() {
        employer = userRepository.save(User.builder()
                .fullName("ACME Corp").email("acme@corp.com")
                .password("hashed").role(User.Role.EMPLOYER).build());

        seeker1 = userRepository.save(User.builder()
                .fullName("Alice").email("alice@test.com")
                .password("hashed").role(User.Role.JOB_SEEKER).build());

        seeker2 = userRepository.save(User.builder()
                .fullName("Bob").email("bob@test.com")
                .password("hashed").role(User.Role.JOB_SEEKER).build());

        job = jobRepository.save(Job.builder()
                .title("Backend Developer").description("Java role")
                .location("Chennai").deadline(LocalDate.now().plusMonths(1))
                .employer(employer).build());

        applicationRepository.save(JobApplication.builder()
                .job(job).applicant(seeker1).coverLetter("Alice's letter").build());

        applicationRepository.save(JobApplication.builder()
                .job(job).applicant(seeker2).coverLetter("Bob's letter").build());
    }

    @Test
    @DisplayName("findByApplicantId() — returns only the given seeker's applications")
    void findByApplicantId_returnsCorrectApplications() {
        List<JobApplication> apps = applicationRepository.findByApplicantId(seeker1.getId());
        assertThat(apps).hasSize(1);
        assertThat(apps.get(0).getApplicant().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("findByJobId() — returns all applications for a job")
    void findByJobId_returnsAllApplicants() {
        List<JobApplication> apps = applicationRepository.findByJobId(job.getId());
        assertThat(apps).hasSize(2);
    }

    @Test
    @DisplayName("findByJobEmployerId() — returns all applications for employer's jobs")
    void findByJobEmployerId_returnsEmployerApplications() {
        List<JobApplication> apps = applicationRepository.findByJobEmployerId(employer.getId());
        assertThat(apps).hasSize(2);
    }

    @Test
    @DisplayName("existsByJobIdAndApplicantId() — true when duplicate application")
    void existsByJobIdAndApplicantId_true_onDuplicate() {
        boolean exists = applicationRepository.existsByJobIdAndApplicantId(
                job.getId(), seeker1.getId());
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByJobIdAndApplicantId() — false for new application")
    void existsByJobIdAndApplicantId_false_forNew() {
        // seeker2 has applied, but checking a non-existent seeker id
        boolean exists = applicationRepository.existsByJobIdAndApplicantId(
                job.getId(), 9999L);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByJobIdAndApplicantId() — returns correct application")
    void findByJobIdAndApplicantId_returnsApplication() {
        Optional<JobApplication> app = applicationRepository.findByJobIdAndApplicantId(
                job.getId(), seeker1.getId());
        assertThat(app).isPresent();
        assertThat(app.get().getCoverLetter()).isEqualTo("Alice's letter");
    }

    @Test
    @DisplayName("default status is PENDING on save")
    void savedApplication_hasDefaultPendingStatus() {
        List<JobApplication> apps = applicationRepository.findByApplicantId(seeker1.getId());
        assertThat(apps.get(0).getStatus()).isEqualTo(JobApplication.ApplicationStatus.PENDING);
    }
}
