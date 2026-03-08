package com.jobtracker.dto;

import com.jobtracker.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationResponse {

    private Long id;
    private String companyName;
    private String roleTitle;
    private ApplicationStatus currentStatus;
    private String source;
    private String jobLink;
    private String location;
    private LocalDate appliedDate;
    private String notes;
    private LocalDate followUpDate;
    private Instant createdAt;
    private Instant updatedAt;
}
