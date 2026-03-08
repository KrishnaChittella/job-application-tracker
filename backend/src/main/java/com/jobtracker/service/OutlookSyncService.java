package com.jobtracker.service;

import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.User;
import com.jobtracker.outlook.EmailStatusParser;
import com.jobtracker.outlook.GraphClient;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Syncs Outlook mailbox: fetches recent emails, applies rule-based status parsing,
 * and optionally creates/updates job applications from detected status updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutlookSyncService {

    private final GraphClient graphClient;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;

    private static final int RECENT_MAIL_TOP = 50;

    /**
     * Result of parsing one email: suggested status and optional company hint.
     */
    public static class ParsedEmail {
        private final String messageId;
        private final String subject;
        private final ApplicationStatus status;
        private final String companyHint;
        private final String receivedDateTime;

        public ParsedEmail(String messageId, String subject, ApplicationStatus status, String companyHint, String receivedDateTime) {
            this.messageId = messageId;
            this.subject = subject;
            this.status = status;
            this.companyHint = companyHint;
            this.receivedDateTime = receivedDateTime;
        }

        public String getMessageId() { return messageId; }
        public String getSubject() { return subject; }
        public ApplicationStatus getStatus() { return status; }
        public String getCompanyHint() { return companyHint; }
        public String getReceivedDateTime() { return receivedDateTime; }
    }

    /**
     * Sync result: list of parsed job-related emails and count of applications created/updated.
     */
    public static class SyncResult {
        private final List<ParsedEmail> parsedEmails;
        private final int applicationsCreated;
        private final int applicationsUpdated;

        public SyncResult(List<ParsedEmail> parsedEmails, int applicationsCreated, int applicationsUpdated) {
            this.parsedEmails = new ArrayList<>(parsedEmails);
            this.applicationsCreated = applicationsCreated;
            this.applicationsUpdated = applicationsUpdated;
        }

        public List<ParsedEmail> getParsedEmails() { return parsedEmails; }
        public int getApplicationsCreated() { return applicationsCreated; }
        public int getApplicationsUpdated() { return applicationsUpdated; }
    }

    @Transactional
    public SyncResult sync(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getOutlookAccessToken() == null || user.getOutlookAccessToken().isBlank()) {
            throw new IllegalStateException("Outlook not connected. Connect your Outlook account first.");
        }

        String accessToken = ensureValidToken(user);
        List<GraphClient.GraphMessage> messages = graphClient.fetchRecentMessages(accessToken, RECENT_MAIL_TOP);

        List<ParsedEmail> parsed = new ArrayList<>();
        int created = 0;
        int updated = 0;

        for (GraphClient.GraphMessage msg : messages) {
            Optional<ApplicationStatus> statusOpt = EmailStatusParser.parseStatus(msg.getSubject(), msg.getBodyContent() + " " + msg.getBodyPreview());
            if (statusOpt.isEmpty()) continue;

            String companyHint = EmailStatusParser.extractCompanyHint(msg.getFromEmail(), msg.getFromName()).orElse("Unknown");
            parsed.add(new ParsedEmail(msg.getId(), msg.getSubject(), statusOpt.get(), companyHint, msg.getReceivedDateTime()));

            // Optionally create or update a job application: match by company hint (simplified)
            Optional<JobApplication> existing = findMatchingApplication(userId, companyHint, msg.getSubject());
            LocalDate appliedDate = parseDate(msg.getReceivedDateTime());

            if (existing.isPresent()) {
                JobApplication app = existing.get();
                if (app.getCurrentStatus() != statusOpt.get()) {
                    app.setCurrentStatus(statusOpt.get());
                    jobApplicationRepository.save(app);
                    updated++;
                }
            } else {
                JobApplication newApp = JobApplication.builder()
                        .user(user)
                        .companyName(companyHint)
                        .roleTitle(extractRoleHint(msg.getSubject()))
                        .currentStatus(statusOpt.get())
                        .source("Outlook")
                        .appliedDate(appliedDate)
                        .notes("Auto-detected from email: " + msg.getSubject())
                        .build();
                jobApplicationRepository.save(newApp);
                created++;
            }
        }

        return new SyncResult(parsed, created, updated);
    }

    private String ensureValidToken(User user) {
        if (user.getOutlookTokenExpiresAt() != null && user.getOutlookTokenExpiresAt().isAfter(Instant.now().plusSeconds(300))) {
            return user.getOutlookAccessToken();
        }
        if (user.getOutlookRefreshToken() == null || user.getOutlookRefreshToken().isBlank()) {
            throw new IllegalStateException("Outlook refresh token missing. Reconnect your account.");
        }
        GraphClient.TokenResponse refreshed = graphClient.refreshTokens(user.getOutlookRefreshToken());
        user.setOutlookAccessToken(refreshed.getAccessToken());
        if (refreshed.getRefreshToken() != null) user.setOutlookRefreshToken(refreshed.getRefreshToken());
        user.setOutlookTokenExpiresAt(Instant.now().plusSeconds(refreshed.getExpiresInSeconds()));
        userRepository.save(user);
        return user.getOutlookAccessToken();
    }

    private Optional<JobApplication> findMatchingApplication(Long userId, String companyHint, String subject) {
        String normalizedCompany = companyHint.toLowerCase().replaceAll("[^a-z0-9]", "");
        List<JobApplication> byUser = jobApplicationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        for (JobApplication app : byUser) {
            String appCompany = (app.getCompanyName() != null ? app.getCompanyName() : "").toLowerCase().replaceAll("[^a-z0-9]", "");
            if (appCompany.contains(normalizedCompany) || normalizedCompany.contains(appCompany)) return Optional.of(app);
            if (subject != null && subject.toLowerCase().contains(appCompany)) return Optional.of(app);
        }
        return Optional.empty();
    }

    private String extractRoleHint(String subject) {
        if (subject == null || subject.isBlank()) return "Position";
        // Use first ~60 chars as hint; user can edit later
        return subject.length() > 60 ? subject.substring(0, 57) + "..." : subject;
    }

    private LocalDate parseDate(String receivedDateTime) {
        if (receivedDateTime == null || receivedDateTime.isBlank()) return LocalDate.now();
        try {
            String normalized = receivedDateTime.replace(" ", "T");
            if (!normalized.endsWith("Z") && !normalized.contains("+") && !normalized.contains("-")) {
                normalized = normalized + "Z";
            }
            Instant i = Instant.parse(normalized);
            return i.atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
