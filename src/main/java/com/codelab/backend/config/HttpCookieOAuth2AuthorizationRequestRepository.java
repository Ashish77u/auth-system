package com.codelab.backend.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE = "oauth2_auth_request";

    private final ObjectMapper objectMapper;

    @Value("${application.oauth2.cookie-expire-seconds}")
    private int cookieExpireSeconds;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
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

    // ── Serialize using Jackson (JSON) instead of Java serialization ──

    private String serialize(OAuth2AuthorizationRequest request) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(request);
            return Base64.getUrlEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("Failed to serialize OAuth2AuthorizationRequest: {}",
                    e.getMessage());
            throw new RuntimeException("Failed to serialize OAuth2 request", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            if (!StringUtils.hasText(value)) return null;
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            return objectMapper.readValue(bytes, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            log.error("Failed to deserialize OAuth2AuthorizationRequest: {}",
                    e.getMessage());
            return null;   // returning null forces a fresh OAuth2 flow
        }
    }
}

//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
//import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
//import org.springframework.stereotype.Component;
//import org.springframework.util.SerializationUtils;
//
//import java.util.Base64;
//
//@Component
//public class HttpCookieOAuth2AuthorizationRequestRepository
//        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
//
//    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE = "oauth2_auth_request";
//
//    @Value("${application.oauth2.cookie-expire-seconds}")
//    private int cookieExpireSeconds;
//
//    @Override
//    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
//        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE)
//                .map(cookie -> deserialize(cookie.getValue()))
//                .orElse(null);
//    }
//
//    @Override
//    public void saveAuthorizationRequest(
//            OAuth2AuthorizationRequest authorizationRequest,
//            HttpServletRequest request,
//            HttpServletResponse response) {
//
//        if (authorizationRequest == null) {
//            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE);
//            return;
//        }
//
//        CookieUtils.addCookie(
//                response,
//                OAUTH2_AUTHORIZATION_REQUEST_COOKIE,
//                serialize(authorizationRequest),
//                cookieExpireSeconds
//        );
//    }
//
//    @Override
//    public OAuth2AuthorizationRequest removeAuthorizationRequest(
//            HttpServletRequest request,
//            HttpServletResponse response) {
//
//        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
//        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE);
//        return authRequest;
//    }
//
//    private String serialize(OAuth2AuthorizationRequest request) {
//        return Base64.getUrlEncoder()
//                .encodeToString(SerializationUtils.serialize(request));
//    }
//
//    private OAuth2AuthorizationRequest deserialize(String value) {
//        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
//                Base64.getUrlDecoder().decode(value));
//    }
//}