package com.jobtracker.repository;

import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Page<JobApplication> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT j FROM JobApplication j WHERE j.user.id = :userId " +
           "AND (:status IS NULL OR j.currentStatus = :status) " +
           "AND (:companySearch IS NULL OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :companySearch, '%')) " +
           "     OR LOWER(j.roleTitle) LIKE LOWER(CONCAT('%', :companySearch, '%'))) " +
           "ORDER BY j.createdAt DESC")
    Page<JobApplication> findByUserIdWithFilters(
        @Param("userId") Long userId,
        @Param("status") ApplicationStatus status,
        @Param("companySearch") String companySearch,
        Pageable pageable
    );

    long countByUserId(Long userId);

    long countByUserIdAndCurrentStatus(Long userId, ApplicationStatus status);

    List<JobApplication> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
