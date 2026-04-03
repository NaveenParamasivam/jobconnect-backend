package com.jobconnect.service;

import com.jobconnect.dto.request.LoginRequest;
import com.jobconnect.dto.request.RegisterRequest;
import com.jobconnect.dto.response.AuthResponse;
import com.jobconnect.entity.User;
import com.jobconnect.exception.BadRequestException;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.service.impl.AuthServiceImpl;
import com.jobconnect.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceTest {

    @Mock UserRepository      userRepository;
    @Mock PasswordEncoder     passwordEncoder;
    @Mock JwtUtil             jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @Mock SmsService          smsService;

    @InjectMocks AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User             savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Jane Seeker");
        registerRequest.setEmail("jane@test.com");
        registerRequest.setPassword("secure123");
        registerRequest.setRole(User.Role.JOB_SEEKER);

        savedUser = User.builder()
                .id(1L).fullName("Jane Seeker").email("jane@test.com")
                .password("hashed").role(User.Role.JOB_SEEKER).build();
    }

    @Test
    @DisplayName("register() — new user returns JWT token")
    void register_newUser_returnsAuthResponse() {
        when(userRepository.existsByEmail("jane@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secure123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("jane@test.com")).thenReturn("jwt.token.value");

        AuthResponse result = authService.register(registerRequest);

        assertThat(result.getToken()).isEqualTo("jwt.token.value");
        assertThat(result.getEmail()).isEqualTo("jane@test.com");
        assertThat(result.getRole()).isEqualTo(User.Role.JOB_SEEKER);
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() — duplicate email throws BadRequestException")
    void register_duplicateEmail_throwsBadRequest() {
        when(userRepository.existsByEmail("jane@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login() — valid credentials return JWT token")
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest req = new LoginRequest();
        req.setEmail("jane@test.com");
        req.setPassword("secure123");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("jane@test.com", null));
        when(userRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken("jane@test.com")).thenReturn("jwt.login.token");

        AuthResponse result = authService.login(req);

        assertThat(result.getToken()).isEqualTo("jwt.login.token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("login() — bad credentials propagate BadCredentialsException")
    void login_badCredentials_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("wrong@test.com");
        req.setPassword("wrongpass");

        doThrow(new BadCredentialsException("Bad creds"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
