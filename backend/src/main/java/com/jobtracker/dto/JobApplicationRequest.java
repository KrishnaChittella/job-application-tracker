package com.jobtracker.dto;

import com.jobtracker.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class JobApplicationRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Role title is required")
    private String roleTitle;

    @NotNull(message = "Status is required")
    private ApplicationStatus currentStatus;

    private String source;
    private String jobLink;
    private String location;
    private LocalDate appliedDate;
    private String notes;
    private LocalDate followUpDate;
}
