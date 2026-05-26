package com.codelab.backend.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.codelab.backend.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Read the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. If no token or wrong format → skip this filter, continue the chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        // 4. Extract the email from the token
        final String userEmail = jwtService.extractUsername(jwt);

        // 5. If we have an email AND the user isn't already authenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load user from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // 7. Validate the token against this user
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 8. Create an authentication token and set it in the SecurityContext
                // This is what tells Spring Security "this request is authenticated"
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials (not needed after auth)
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 9. Continue to the next filter / controller
        filterChain.doFilter(request, response);
    }
}


/*

    *** Why OncePerRequestFilter? ***
    - OncePerRequestFilter ensures that this filter is executed only once per request. This is important because we don't want to accidentally authenticate the same request multiple times, which could lead to performance issues or unexpected behavior. By extending OncePerRequestFilter, we guarantee that our JWT authentication logic runs just once for each incoming HTTP request.

    *** JwtAuthenticationFilter is responsible for intercepting incoming HTTP requests, extracting the JWT token from the Authorization header, validating it, and if valid, setting the authentication in the Spring Security context. This allows our application to recognize the user associated with the token and apply security rules accordingly. It's a crucial part of our stateless authentication mechanism using JWTs.

    

*/