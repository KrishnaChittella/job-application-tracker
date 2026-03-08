package com.jobtracker.service;

import com.jobtracker.dto.DashboardResponse;
import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.User;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> getApplications(Long userId, ApplicationStatus status,
                                                        String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String companySearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<JobApplication> pageResult = jobApplicationRepository.findByUserIdWithFilters(
                userId, status, companySearch, pageable);
        return pageResult.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse getById(Long id, Long userId) {
        JobApplication app = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!app.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Application not found");
        }
        return toResponse(app);
    }

    @Transactional
    public JobApplicationResponse create(Long userId, JobApplicationRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        JobApplication app = toEntity(user, request);
        app = jobApplicationRepository.save(app);
        return toResponse(app);
    }

    @Transactional
    public JobApplicationResponse update(Long id, Long userId, JobApplicationRequest request) {
        JobApplication app = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!app.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Application not found");
        }
        updateEntity(app, request);
        app = jobApplicationRepository.save(app);
        return toResponse(app);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        JobApplication app = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!app.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Application not found");
        }
        jobApplicationRepository.delete(app);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {
        long total = jobApplicationRepository.countByUserId(userId);
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, jobApplicationRepository.countByUserIdAndCurrentStatus(userId, status));
        }
        List<JobApplication> recent = jobApplicationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        List<JobApplicationResponse> recentResponses = recent.stream().map(this::toResponse).collect(Collectors.toList());
        return DashboardResponse.builder()
                .totalApplications(total)
                .countsByStatus(counts)
                .recentApplications(recentResponses)
                .build();
    }

    private JobApplicationResponse toResponse(JobApplication app) {
        return JobApplicationResponse.builder()
                .id(app.getId())
                .companyName(app.getCompanyName())
                .roleTitle(app.getRoleTitle())
                .currentStatus(app.getCurrentStatus())
                .source(app.getSource())
                .jobLink(app.getJobLink())
                .location(app.getLocation())
                .appliedDate(app.getAppliedDate())
                .notes(app.getNotes())
                .followUpDate(app.getFollowUpDate())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private JobApplication toEntity(User user, JobApplicationRequest req) {
        return JobApplication.builder()
                .user(user)
                .companyName(req.getCompanyName())
                .roleTitle(req.getRoleTitle())
                .currentStatus(req.getCurrentStatus())
                .source(req.getSource())
                .jobLink(req.getJobLink())
                .location(req.getLocation())
                .appliedDate(req.getAppliedDate())
                .notes(req.getNotes())
                .followUpDate(req.getFollowUpDate())
                .build();
    }

    private void updateEntity(JobApplication app, JobApplicationRequest req) {
        app.setCompanyName(req.getCompanyName());
        app.setRoleTitle(req.getRoleTitle());
        app.setCurrentStatus(req.getCurrentStatus());
        app.setSource(req.getSource());
        app.setJobLink(req.getJobLink());
        app.setLocation(req.getLocation());
        app.setAppliedDate(req.getAppliedDate());
        app.setNotes(req.getNotes());
        app.setFollowUpDate(req.getFollowUpDate());
    }
}
