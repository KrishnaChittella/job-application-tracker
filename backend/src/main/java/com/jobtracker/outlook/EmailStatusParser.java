package com.jobtracker.outlook;

import com.jobtracker.entity.ApplicationStatus;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Rule-based keyword parsing to classify email content into job application status.
 * No paid AI APIs; uses keyword and phrase matching only.
 */
public final class EmailStatusParser {

    private EmailStatusParser() {}

    // Case-insensitive patterns for each status
    private static final Pattern[] REJECTED_PATTERNS = compile(
            "we have decided to move forward with other", "we will not be moving forward",
            "unfortunately we will not", "not selected to move forward", "position has been filled",
            "other candidates whose qualifications", "we regret to inform", "not moving forward with your application",
            "thank you for your interest.*at this time", "decided to pursue other candidates");

    private static final Pattern[] OFFER_PATTERNS = compile(
            "we are pleased to offer", "offer of employment", "job offer", "we would like to extend an offer",
            "congratulations.*offer", "pleased to extend", "formal offer", "offer letter");

    private static final Pattern[] INTERVIEW_PATTERNS = compile(
            "interview.*scheduled", "schedule.*interview", "invite you.*interview", "interview invitation",
            "round of interview", "next steps.*interview", "interview with", "meeting invite.*interview",
            "calendar invite.*interview", "you have been selected for an interview");

    private static final Pattern[] ASSESSMENT_PATTERNS = compile(
            "online assessment", "coding assessment", "technical assessment", "complete the following assessment",
            "assessment link", "invitation to complete", "next step.*assessment", "hackerrank", "codility",
            "take-home", "take home assignment");

    private static final Pattern[] APPLIED_PATTERNS = compile(
            "application received", "we have received your application", "thank you for applying",
            "application submitted", "your application has been received");

    /**
     * Tries to infer a status from email subject + body. Order of check: REJECTED, OFFER, INTERVIEW, ASSESSMENT, APPLIED.
     */
    public static Optional<ApplicationStatus> parseStatus(String subject, String body) {
        String combined = (subject == null ? "" : subject) + " " + (body == null ? "" : body);
        String normalized = combined.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");

        if (matchesAny(normalized, REJECTED_PATTERNS)) return Optional.of(ApplicationStatus.REJECTED);
        if (matchesAny(normalized, OFFER_PATTERNS)) return Optional.of(ApplicationStatus.OFFER);
        if (matchesAny(normalized, INTERVIEW_PATTERNS)) return Optional.of(ApplicationStatus.INTERVIEW);
        if (matchesAny(normalized, ASSESSMENT_PATTERNS)) return Optional.of(ApplicationStatus.ASSESSMENT);
        if (matchesAny(normalized, APPLIED_PATTERNS)) return Optional.of(ApplicationStatus.APPLIED);

        return Optional.empty();
    }

    /**
     * Tries to extract a company name from sender address or display name (e.g. "recruiter@company.com" -> "company").
     */
    public static Optional<String> extractCompanyHint(String fromEmail, String fromDisplayName) {
        if (fromEmail != null && !fromEmail.isBlank()) {
            int at = fromEmail.indexOf('@');
            if (at > 0 && at < fromEmail.length() - 1) {
                String domain = fromEmail.substring(at + 1).toLowerCase(Locale.ENGLISH);
                // Remove common suffixes like .com, mail. company.com
                String name = domain.replaceFirst("^(mail\\.|email\\.|careers\\.|jobs\\.|recruiting\\.)", "");
                name = name.replaceFirst("\\.(com|io|co|org|net)$", "");
                if (!name.isBlank()) return Optional.of(name);
            }
        }
        if (fromDisplayName != null && !fromDisplayName.isBlank()) {
            return Optional.of(fromDisplayName.trim());
        }
        return Optional.empty();
    }

    private static Pattern[] compile(String... regexes) {
        Pattern[] out = new Pattern[regexes.length];
        for (int i = 0; i < regexes.length; i++) {
            out[i] = Pattern.compile(regexes[i], Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }
        return out;
    }

    private static boolean matchesAny(String text, Pattern[] patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(text).find()) return true;
        }
        return false;
    }
}
