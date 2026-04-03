package com.jobconnect.dto.response;

import com.jobconnect.entity.Job;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private Long id;
    private String title;
    private String description;
    private String location;
    private Double salary;
    private LocalDate deadline;
    private String jobType;
    private String category;
    private Job.JobStatus status;
    private Long employerId;
    private String employerName;
    private LocalDateTime createdAt;

    public static JobResponse fromEntity(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .salary(job.getSalary())
                .deadline(job.getDeadline())
                .jobType(job.getJobType())
                .category(job.getCategory())
                .status(job.getStatus())
                .employerId(job.getEmployer().getId())
                .employerName(job.getEmployer().getFullName())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
