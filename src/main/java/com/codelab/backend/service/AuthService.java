package com.codelab.backend.service;

import com.codelab.backend.dto.AuthResponse;
import com.codelab.backend.dto.LoginRequest;
import com.codelab.backend.dto.RegisterRequest;
import com.codelab.backend.entity.User;
import com.codelab.backend.enums.Role;
import com.codelab.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {

        // 1. Check if email or username already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        // 2. Build and save the user (enabled = false until email verified)
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .role(Role.USER)
                .enabled(false)      // ← cannot login until email verified
                .provider("local")
                .build();

        userRepository.save(user);

        // 3. TODO Phase 3: send verification email here

        return AuthResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .emailVerified(false)
                .message("Registration successful! Please check your email to verify your account.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        // 1. This line does everything: loads user, checks password, checks isEnabled()
        //    Throws AuthenticationException if anything fails
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. If we reach here, authentication succeeded — load the user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Generate JWT
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .emailVerified(user.isEnabled())
                .message("Login successful")
                .build();
    }
}
