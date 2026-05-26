package com.codelab.backend.service;

import com.codelab.backend.entity.User;
import com.codelab.backend.entity.VerificationToken;
import com.codelab.backend.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;

    // Creates a fresh token tied to the user, valid for 24 hours
    public VerificationToken createToken(User user) {

        // If user already has a token (e.g. resend request), delete the old one first
        tokenRepository.deleteByUser_Id(user.getId());

        VerificationToken token = VerificationToken.builder()
                .token(UUID.randomUUID().toString())   // random UUID like "a3f9b2c1-..."
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        return tokenRepository.save(token);
    }

    // Finds the token entity — throws if not found
    public VerificationToken findByToken(String token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
    }

    // Deletes the token after successful use — one-time use only
    @Transactional
    public void deleteToken(VerificationToken token) {
        tokenRepository.delete(token);
    }
}
