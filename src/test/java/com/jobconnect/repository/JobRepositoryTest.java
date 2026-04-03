package com.jobconnect.repository;

import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("JobRepository — Integration Tests")
class JobRepositoryTest {

    @Autowired JobRepository  jobRepository;
    @Autowired UserRepository userRepository;

    private User employer;

    @BeforeEach
    void setUp() {
        employer = userRepository.save(User.builder()
                .fullName("ACME Corp").email("acme@corp.com")
                .password("hashed").role(User.Role.EMPLOYER).build());

        jobRepository.save(Job.builder()
                .title("Backend Developer").description("Java Spring Boot")
                .location("Chennai").salary(80000.0)
                .deadline(LocalDate.now().plusMonths(1))
                .jobType("FULL_TIME").category("Engineering")
                .employer(employer).build());

        jobRepository.save(Job.builder()
                .title("Frontend Developer").description("React TypeScript")
                .location("Bangalore").salary(75000.0)
                .deadline(LocalDate.now().plusMonths(2))
                .jobType("REMOTE").category("Engineering")
                .employer(employer).build());
    }

    @Test
    @DisplayName("findByStatus() — returns only ACTIVE jobs")
    void findByStatus_active_returnsCorrectCount() {
        List<Job> activeJobs = jobRepository.findByStatus(Job.JobStatus.ACTIVE);
        assertThat(activeJobs).hasSize(2);
    }

    @Test
    @DisplayName("search() — keyword match in title")
    void search_byKeyword_returnsMatches() {
        List<Job> results = jobRepository.search("Backend", null, null, null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Backend Developer");
    }

    @Test
    @DisplayName("search() — location filter")
    void search_byLocation_returnsMatches() {
        List<Job> results = jobRepository.search(null, "Bangalore", null, null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLocation()).isEqualTo("Bangalore");
    }

    @Test
    @DisplayName("search() — jobType filter")
    void search_byJobType_returnsMatches() {
        List<Job> results = jobRepository.search(null, null, null, "REMOTE");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getJobType()).isEqualTo("REMOTE");
    }

    @Test
    @DisplayName("findByEmployerId() — returns employer's own jobs")
    void findByEmployerId_returnsOwnJobs() {
        List<Job> jobs = jobRepository.findByEmployerId(employer.getId());
        assertThat(jobs).hasSize(2);
    }
}
