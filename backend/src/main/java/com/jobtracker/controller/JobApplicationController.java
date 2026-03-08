package com.jobtracker.controller;

import com.jobtracker.dto.DashboardResponse;
import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.security.UserPrincipal;
import com.jobtracker.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Job Applications", description = "CRUD and list job applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @GetMapping
    @Operation(summary = "List applications with optional filters and search")
    public Page<JobApplicationResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobApplicationService.getApplications(
                principal.getId(), status, search, page, size);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard counts and recent applications")
    public ResponseEntity<DashboardResponse> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        DashboardResponse dashboard = jobApplicationService.getDashboard(principal.getId());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one application by id")
    public ResponseEntity<JobApplicationResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        JobApplicationResponse response = jobApplicationService.getById(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new application")
    public ResponseEntity<JobApplicationResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JobApplicationRequest request) {
        JobApplicationResponse response = jobApplicationService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an application")
    public ResponseEntity<JobApplicationResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationRequest request) {
        JobApplicationResponse response = jobApplicationService.update(id, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an application")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        jobApplicationService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
