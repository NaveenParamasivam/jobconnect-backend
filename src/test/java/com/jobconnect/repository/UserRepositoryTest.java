package com.jobconnect.repository;

import com.jobconnect.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository — Integration Tests")
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(User.builder()
                .fullName("Jane Seeker").email("jane@test.com")
                .password("hashed123").role(User.Role.JOB_SEEKER).build());
    }

    @Test
    @DisplayName("findByEmail() — returns user when email exists")
    void findByEmail_existingEmail_returnsUser() {
        Optional<User> result = userRepository.findByEmail("jane@test.com");
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Jane Seeker");
        assertThat(result.get().getRole()).isEqualTo(User.Role.JOB_SEEKER);
    }

    @Test
    @DisplayName("findByEmail() — returns empty for unknown email")
    void findByEmail_unknownEmail_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("ghost@test.com");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail() — true when email already registered")
    void existsByEmail_registeredEmail_returnsTrue() {
        assertThat(userRepository.existsByEmail("jane@test.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail() — false for new email")
    void existsByEmail_newEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("newuser@test.com")).isFalse();
    }

    @Test
    @DisplayName("save() — persists employer with correct role")
    void save_employer_persistsWithCorrectRole() {
        User employer = userRepository.save(User.builder()
                .fullName("ACME Corp").email("acme@corp.com")
                .password("hashed").role(User.Role.EMPLOYER).build());

        assertThat(employer.getId()).isNotNull();
        assertThat(employer.getRole()).isEqualTo(User.Role.EMPLOYER);
        assertThat(employer.isEnabled()).isTrue();
    }
}
