package com.aventra.repository;

import com.aventra.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailOrderByIdDesc(String email);

    Optional<OtpVerification> findTopByEmailAndVerifiedTrueOrderByIdDesc(
            String email
    );
}
