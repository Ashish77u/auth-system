package com.codelab.backend.service;

import com.codelab.backend.dto.AuthResponse;
import com.codelab.backend.dto.LoginRequest;
import com.codelab.backend.dto.RegisterRequest;
import com.codelab.backend.entity.User;
import com.codelab.backend.entity.VerificationToken;
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
    private final VerificationTokenService verificationTokenService;  // ← new
    private final EmailService emailService;                          // ← new

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(false)
                .provider("local")
                .build();

        userRepository.save(user);

        // ── NEW: create token and send email ──────────────────────────
        VerificationToken verificationToken = verificationTokenService.createToken(user);

        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getUsername(),
                verificationToken.getToken()
        );
        // ─────────────────────────────────────────────────────────────

        return AuthResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .emailVerified(false)
                .message("Registration successful! Please check your email to verify your account.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        // Throws BadCredentialsException or DisabledException if anything fails
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

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