package com.jobtracker.dto;

import com.jobtracker.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private long totalApplications;
    private Map<ApplicationStatus, Long> countsByStatus;
    private List<JobApplicationResponse> recentApplications;
}
