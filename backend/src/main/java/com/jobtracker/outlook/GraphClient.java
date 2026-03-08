package com.jobtracker.outlook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Microsoft Graph API client: token exchange and mail read.
 * Uses RestTemplate (no extra SDK) to keep dependencies minimal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphClient {

    private static final String TOKEN_URL_TEMPLATE = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String GRAPH_ME = "https://graph.microsoft.com/v1.0/me";
    private static final String GRAPH_MAIL = "https://graph.microsoft.com/v1.0/me/messages";
    private static final String SCOPES = "openid profile email User.Read Mail.Read offline_access";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${azure.client-id:}")
    private String clientId;

    @Value("${azure.client-secret:}")
    private String clientSecret;

    @Value("${azure.tenant-id:}")
    private String tenantId;

    @Value("${azure.redirect-uri:}")
    private String redirectUri;

    public String buildAuthorizeUrl(String state) {
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedScopes = URLEncoder.encode(SCOPES, StandardCharsets.UTF_8);
        String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
        return String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s&response_mode=query",
                tenantId, clientId, encodedRedirect, encodedScopes, encodedState);
    }

    /**
     * Exchange authorization code for access and refresh tokens.
     */
    public TokenResponse exchangeCodeForTokens(String code) {
        String url = String.format(TOKEN_URL_TEMPLATE, tenantId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Token exchange failed: " + response.getStatusCode());
        }
        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            return TokenResponse.builder()
                    .accessToken(node.path("access_token").asText(null))
                    .refreshToken(node.path("refresh_token").asText(null))
                    .expiresInSeconds(node.path("expires_in").asLong(3600))
                    .build();
        } catch (Exception e) {
            log.error("Parse token response failed", e);
            throw new RuntimeException("Token exchange failed", e);
        }
    }

    /**
     * Refresh access token using refresh_token.
     */
    public TokenResponse refreshTokens(String refreshToken) {
        String url = String.format(TOKEN_URL_TEMPLATE, tenantId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Token refresh failed: " + response.getStatusCode());
        }
        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            return TokenResponse.builder()
                    .accessToken(node.path("access_token").asText(null))
                    .refreshToken(node.path("refresh_token").asText(null))
                    .expiresInSeconds(node.path("expires_in").asLong(3600))
                    .build();
        } catch (Exception e) {
            log.error("Parse refresh response failed", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Fetch recent mail messages (subject, bodyPreview, from). Uses access token in Authorization header.
     */
    public List<GraphMessage> fetchRecentMessages(String accessToken, int top) {
        String url = GRAPH_MAIL + "?$top=" + top + "&$orderby=receivedDateTime desc&$select=id,subject,bodyPreview,body,from,receivedDateTime";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Graph mail request failed: " + response.getStatusCode());
        }
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode value = root.path("value");
            List<GraphMessage> messages = new ArrayList<>();
            for (JsonNode msg : value) {
                String id = msg.path("id").asText(null);
                String subject = msg.path("subject").asText("");
                String bodyPreview = msg.path("bodyPreview").asText("");
                String bodyContent = "";
                if (msg.has("body") && msg.get("body").has("content")) {
                    bodyContent = msg.get("body").get("content").asText("");
                }
                String fromEmail = "";
                String fromName = "";
                JsonNode from = msg.path("from").path("emailAddress");
                if (from.has("address")) fromEmail = from.path("address").asText("");
                if (from.has("name")) fromName = from.path("name").asText("");
                String received = msg.path("receivedDateTime").asText(null);
                messages.add(new GraphMessage(id, subject, bodyPreview, bodyContent, fromEmail, fromName, received));
            }
            return messages;
        } catch (Exception e) {
            log.error("Parse Graph mail response failed", e);
            throw new RuntimeException("Failed to parse mail response", e);
        }
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && tenantId != null && !tenantId.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }

    @lombok.Data
    @lombok.Builder
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private long expiresInSeconds;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class GraphMessage {
        private String id;
        private String subject;
        private String bodyPreview;
        private String bodyContent;
        private String fromEmail;
        private String fromName;
        private String receivedDateTime;
    }
}
