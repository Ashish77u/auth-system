package com.codelab.backend.repository;

import com.codelab.backend.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<com.codelab.backend.entity.VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    void deleteByUser_Id(Long userId);
}