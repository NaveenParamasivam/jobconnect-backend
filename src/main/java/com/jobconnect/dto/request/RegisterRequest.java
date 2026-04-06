package com.jobconnect.dto.request;

import com.jobconnect.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Must be a valid international phone number")
    private String phoneNumber;

    @NotNull(message = "Role is required (JOB_SEEKER or EMPLOYER)")
    private User.Role role;
}
