package com.jobtracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "job_applications", indexes = {
    @Index(name = "idx_job_app_user_id", columnList = "user_id"),
    @Index(name = "idx_job_app_status", columnList = "current_status"),
    @Index(name = "idx_job_app_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "role_title", nullable = false, length = 255)
    private String roleTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 50)
    private ApplicationStatus currentStatus;

    @Column(length = 100)
    private String source;

    @Column(name = "job_link", length = 1024)
    private String jobLink;

    @Column(length = 255)
    private String location;

    @Column(name = "applied_date")
    private LocalDate appliedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
