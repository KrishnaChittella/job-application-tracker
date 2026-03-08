package com.jobtracker.dto;

import com.jobtracker.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutlookSyncResultDto {

    private List<ParsedEmailDto> parsedEmails;
    private int applicationsCreated;
    private int applicationsUpdated;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedEmailDto {
        private String messageId;
        private String subject;
        private ApplicationStatus status;
        private String companyHint;
        private String receivedDateTime;
    }
}
