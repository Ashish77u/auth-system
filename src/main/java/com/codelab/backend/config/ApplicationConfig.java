package com.codelab.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.codelab.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    // Tells Spring Security HOW to load a user — by email from our DB
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // Wires together: UserDetailsService + PasswordEncoder
    // Spring Security uses this to authenticate login attempts
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // AuthenticationManager is what actually triggers the authentication process
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // BCrypt is the industry standard for hashing passwords
    // Never store plain-text passwords — BCrypt adds a salt and is slow by design
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}



/*

    *** 

    ApplicationConfig is where we define beans related to authentication and user management. It tells Spring Security how to load users (by email), how to authenticate them (using DaoAuthenticationProvider), and how to encode passwords (using BCrypt). This is crucial for the login and registration functionality of our application.

    -- ApplicationConfig important hai kyunki isme humne authentication ke liye beans define kiye hain.
       UserDetailsService batata hai ki user ko kaise load karna hai (email se), AuthenticationProvider batata hai ki authentication process kaise chalana hai,
       aur PasswordEncoder batata hai ki passwords ko kaise hash karna hai (BCrypt ke saath). Ye sab login aur registration ke liye zaroori hai.

    


*/