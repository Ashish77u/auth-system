package com.codelab.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfigurationSource;  // ← add this


    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))  // ← add this
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }
}



/*

    *** step-by-step explanation ***

    1. We define a SecurityFilterChain bean that configures Spring Security's HTTP security.
    2. We disable CSRF protection since we're using JWTs and not sessions/cookies. This is important for stateless authentication. ( CSRF disable karna zaroori hai kyunki hum JWTs use kar rahe hain, sessions/cookies nahi. Ye stateless authentication ke liye important hai. )
    3. We specify which endpoints are public (like /api/auth/** for login/register and Swagger docs) and require authentication for all other endpoints.
    4. We set the session management to STATELESS, which means Spring Security won't create or use HTTP sessions. This is crucial for JWT-based authentication.
    5. We tell Spring Security to use our custom AuthenticationProvider, which is configured to load users from the database and check passwords.
    6. We add our custom JwtAuthenticationFilter to the filter chain, ensuring it runs before the default UsernamePasswordAuthenticationFilter. This filter will check incoming requests for a valid JWT and set the authentication context accordingly.

    *** SecurityConfig is the central place where we configure how Spring Security should handle authentication and authorization in our application. It defines which endpoints are public, how to manage sessions (stateless with JWT), which authentication provider to use, and adds our custom JWT filter to the security filter chain. This configuration is essential for securing our API endpoints and ensuring that only authenticated users can access protected resources.
    



*/
