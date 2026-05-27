package com.codelab.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE = "oauth2_auth_request";
    public static final String REDIRECT_URI_COOKIE = "redirect_uri";

    @Value("${application.oauth2.cookie-expire-seconds}")
    private int cookieExpireSeconds;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(
            HttpServletRequest request) {

        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE)
                .map(cookie -> deserialize(cookie.getValue()))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response,
                    OAUTH2_AUTHORIZATION_REQUEST_COOKIE);
            CookieUtils.deleteCookie(request, response,
                    REDIRECT_URI_COOKIE);
            return;
        }

        CookieUtils.addCookie(
                response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE,
                serialize(authorizationRequest),
                cookieExpireSeconds
        );
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        CookieUtils.deleteCookie(request, response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE);
        return authRequest;
    }

    // ── Serialization — store only what Spring needs to rebuild the request ──

    private String serialize(OAuth2AuthorizationRequest request) {
        try {
            // Build a simple key=value string with the fields Spring needs
            StringBuilder sb = new StringBuilder();
            sb.append("authorizationUri=")
                    .append(encode(request.getAuthorizationUri())).append("|");
            sb.append("clientId=")
                    .append(encode(request.getClientId())).append("|");
            sb.append("redirectUri=")
                    .append(encode(request.getRedirectUri())).append("|");
            sb.append("state=")
                    .append(encode(request.getState())).append("|");
            sb.append("scopes=")
                    .append(encode(String.join(",", request.getScopes())));

            // Additional params (code_challenge etc.)
            if (request.getAdditionalParameters() != null &&
                    !request.getAdditionalParameters().isEmpty()) {
                StringBuilder params = new StringBuilder();
                request.getAdditionalParameters().forEach((k, v) ->
                        params.append(k).append(":").append(v).append(";"));
                sb.append("|additionalParams=")
                        .append(encode(params.toString()));
            }

            return Base64.getUrlEncoder().encodeToString(
                    sb.toString().getBytes());

        } catch (Exception e) {
            log.error("Failed to serialize OAuth2 request: {}", e.getMessage());
            throw new RuntimeException("Serialization failed", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value));
            Map<String, String> fields = new HashMap<>();

            for (String part : decoded.split("\\|")) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    fields.put(part.substring(0, idx),
                            decode(part.substring(idx + 1)));
                }
            }

            // Rebuild the OAuth2AuthorizationRequest from stored fields
            OAuth2AuthorizationRequest.Builder builder =
                    OAuth2AuthorizationRequest.authorizationCode()
                            .authorizationUri(fields.get("authorizationUri"))
                            .clientId(fields.get("clientId"))
                            .redirectUri(fields.get("redirectUri"))
                            .state(fields.get("state"))
                            .scopes(java.util.Set.of(
                                    fields.get("scopes").split(",")));

            // Restore additional parameters if present
            if (fields.containsKey("additionalParams")) {
                Map<String, Object> additionalParams = new HashMap<>();
                String paramsStr = fields.get("additionalParams");
                if (paramsStr != null && !paramsStr.isEmpty()) {
                    for (String param : paramsStr.split(";")) {
                        if (param.contains(":")) {
                            String[] kv = param.split(":", 2);
                            additionalParams.put(kv[0], kv[1]);
                        }
                    }
                }
                if (!additionalParams.isEmpty()) {
                    builder.additionalParameters(additionalParams);
                }
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to deserialize OAuth2 request: {}", e.getMessage());
            return null; // Forces a fresh OAuth2 flow
        }
    }

    private String encode(String value) {
        if (value == null) return "";
        return Base64.getUrlEncoder().encodeToString(value.getBytes());
    }

    private String decode(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Base64.getUrlDecoder().decode(value));
    }
}

