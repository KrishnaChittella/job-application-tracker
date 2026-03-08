package com.jobtracker.controller;

import com.jobtracker.dto.OutlookSyncResultDto;
import com.jobtracker.outlook.GraphClient;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.security.JwtUtil;
import com.jobtracker.security.UserPrincipal;
import com.jobtracker.service.OutlookSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Outlook / Microsoft Graph: authorize URL, OAuth callback (store tokens), and sync.
 */
@RestController
@RequestMapping("/api/outlook")
@RequiredArgsConstructor
@Tag(name = "Outlook", description = "Connect Outlook and sync job-related emails")
public class OutlookController {

    private final GraphClient graphClient;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OutlookSyncService outlookSyncService;

    @GetMapping("/authorize")
    @Operation(summary = "Get Microsoft OAuth authorize URL. Frontend redirects user here.")
    public ResponseEntity<AuthorizeResponse> getAuthorizeUrl(@RequestParam String token) {
        if (!graphClient.isConfigured()) {
            return ResponseEntity.badRequest().build();
        }
        // State = JWT so we can identify user in callback without exposing userId
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            return ResponseEntity.badRequest().build();
        }
        String url = graphClient.buildAuthorizeUrl(token);
        return ResponseEntity.ok(new AuthorizeResponse(url));
    }

    @PostMapping("/callback")
    @Operation(summary = "Exchange code for tokens and store for user. Called by backend with code + state (JWT).")
    public ResponseEntity<CallbackResponse> callback(@RequestBody CallbackRequest request) {
        if (!graphClient.isConfigured() || request.getCode() == null || request.getState() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!jwtUtil.validateToken(request.getState())) {
            return ResponseEntity.status(401).body(new CallbackResponse(false, "Invalid state token"));
        }
        Long userId = jwtUtil.getUserIdFromToken(request.getState());
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(new CallbackResponse(false, "User not found"));
        }
        try {
            GraphClient.TokenResponse tokens = graphClient.exchangeCodeForTokens(request.getCode());
            user.setOutlookAccessToken(tokens.getAccessToken());
            user.setOutlookRefreshToken(tokens.getRefreshToken());
            user.setOutlookTokenExpiresAt(Instant.now().plusSeconds(tokens.getExpiresInSeconds()));
            userRepository.save(user);
            return ResponseEntity.ok(new CallbackResponse(true, null));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new CallbackResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Check if Outlook is connected and if Graph app is configured")
    public ResponseEntity<OutlookStatusResponse> status(@AuthenticationPrincipal UserPrincipal principal) {
        var user = userRepository.findById(principal.getId()).orElse(null);
        boolean configured = graphClient.isConfigured();
        boolean connected = user != null && user.getOutlookAccessToken() != null && !user.getOutlookAccessToken().isBlank();
        return ResponseEntity.ok(new OutlookStatusResponse(configured, connected));
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync mailbox: fetch recent emails, parse status, create/update applications")
    public ResponseEntity<OutlookSyncResultDto> sync(@AuthenticationPrincipal UserPrincipal principal) {
        OutlookSyncService.SyncResult result = outlookSyncService.sync(principal.getId());
        OutlookSyncResultDto dto = OutlookSyncResultDto.builder()
                .parsedEmails(result.getParsedEmails().stream()
                        .map(p -> new OutlookSyncResultDto.ParsedEmailDto(
                                p.getMessageId(), p.getSubject(), p.getStatus(), p.getCompanyHint(), p.getReceivedDateTime()))
                        .collect(Collectors.toList()))
                .applicationsCreated(result.getApplicationsCreated())
                .applicationsUpdated(result.getApplicationsUpdated())
                .build();
        return ResponseEntity.ok(dto);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AuthorizeResponse {
        private String authorizeUrl;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CallbackRequest {
        private String code;
        private String state;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CallbackResponse {
        private boolean success;
        private String error;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class OutlookStatusResponse {
        private boolean configured;
        private boolean connected;
    }
}
