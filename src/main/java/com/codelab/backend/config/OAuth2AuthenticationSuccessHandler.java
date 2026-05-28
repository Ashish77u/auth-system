package com.codelab.backend.config;

import com.codelab.backend.entity.RefreshToken;
import com.codelab.backend.entity.User;
import com.codelab.backend.service.JwtService;
import com.codelab.backend.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final HttpCookieOAuth2AuthorizationRequestRepository authRequestRepository;

    private final RefreshTokenService refreshTokenService;                  // <- add


    @Value("${application.oauth2.authorized-redirect-uri}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        // 1. Get the authenticated User object
        User user = (User) authentication.getPrincipal();

        // 2. Generate JWT
        String token = jwtService.generateToken(user);
        RefreshToken refresh = refreshTokenService.createRefreshToken(user); // ← new


        log.info("OAuth2 login successful for: {}", user.getEmail());

        // 3. Clean up the OAuth2 cookie
        clearAuthenticationAttributes(request, response);

        // 4. Redirect to frontend with token as query param
        // e.g. http://localhost:5173/oauth2/callback?token=eyJ...
        String redirectUrl = UriComponentsBuilder
                .fromUriString(authorizedRedirectUri)
                .queryParam("token", token)
                .queryParam("refreshToken", refresh.getToken())               // ← new
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void clearAuthenticationAttributes(
            HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authRequestRepository.removeAuthorizationRequest(request, response);
    }
}