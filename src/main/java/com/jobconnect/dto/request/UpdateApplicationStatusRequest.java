package com.jobconnect.dto.request;

import com.jobconnect.entity.JobApplication;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateApplicationStatusRequest {
    @NotNull(message = "Status is required")
    private JobApplication.ApplicationStatus status;
}
