package com.codelab.backend.controller;

import com.codelab.backend.dto.*;
import com.codelab.backend.entity.PasswordResetToken;
import com.codelab.backend.entity.RefreshToken;
import com.codelab.backend.entity.User;
import com.codelab.backend.entity.VerificationToken;
import com.codelab.backend.repository.PasswordResetTokenRepository;
import com.codelab.backend.repository.UserRepository;
import com.codelab.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final VerificationTokenService verificationTokenService;  // ← new
    private final EmailService emailService;                          // ← new
    private final UserRepository userRepository;                      // ← new

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }


    // ── NEW: user clicks the link in their email ──────────────────────
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {

        // 1. Find the token in DB
        VerificationToken verificationToken = verificationTokenService.findByToken(token);

        // 2. Check if it's expired
        if (verificationToken.isExpired()) {
            verificationTokenService.deleteToken(verificationToken);
            throw new RuntimeException("Verification link has expired. Please request a new one.");
        }

        // 3. Enable the user
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // 4. Delete the token — one-time use
        verificationTokenService.deleteToken(verificationToken);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully! You can now log in."
        ));
    }

    // ── NEW: resend verification email ───────────────────────────────
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        if (user.isEnabled()) {
            throw new RuntimeException("This account is already verified");
        }

        VerificationToken newToken = verificationTokenService.createToken(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), newToken.getToken());

        return ResponseEntity.ok(Map.of(
                "message", "Verification email sent. Please check your inbox."
        ));
    }

    // inject RefreshTokenService, JwtService, UserRepository at top of AuthController

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenService.findByToken(
                request.getRefreshToken());

        if (refreshToken.isExpired()) {
            refreshTokenService.deleteByUser(refreshToken.getUser());
            throw new RuntimeException("Refresh token expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken.getToken()) // same refresh token
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .emailVerified(user.isEnabled())
                .message("Token refreshed")
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenService.findByToken(
                request.getRefreshToken());
        refreshTokenService.deleteByUser(refreshToken.getUser());

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        // Always return success — never reveal if email exists
        userRepository.findByEmail(email).ifPresent(user -> {
            // Only local accounts can reset password
            if ("local".equals(user.getProvider())) {
                passwordResetTokenRepository.deleteByUserId(user.getId());

                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .token(UUID.randomUUID().toString())
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .build();

                passwordResetTokenRepository.save(resetToken);
                emailService.sendPasswordResetEmail(
                        user.getEmail(),
                        user.getUsername(),
                        resetToken.getToken()
                );
            }
        });

        return ResponseEntity.ok(Map.of(
                "message", "If that email exists, a reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        // Invalidate all refresh tokens so old sessions are killed
        refreshTokenService.deleteByUser(user);

        return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully. You can now log in."
        ));
    }
}