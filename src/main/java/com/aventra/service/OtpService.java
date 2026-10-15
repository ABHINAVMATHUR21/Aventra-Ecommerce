package com.aventra.service;

import com.aventra.dto.OtpRequest;
import com.aventra.dto.OtpVerifyRequest;
import com.aventra.entity.OtpVerification;
import com.aventra.repository.OtpVerificationRepository;
import com.aventra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    public OtpService(
            OtpVerificationRepository otpVerificationRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public Map<String, String> sendOtp(OtpRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered");
        }

        String otp = generateOtp();

        OtpVerification verification = new OtpVerification();
        verification.setEmail(request.getEmail());
        verification.setOtp(otp);
        verification.setExpiresAt(
                LocalDateTime.now().plusMinutes(otpExpiryMinutes)
        );
        verification.setVerified(false);

        otpVerificationRepository.save(verification);
        emailService.sendOtpEmail(request.getEmail(), otp);

        return Map.of("message", "OTP sent successfully");
    }

    public Map<String, String> verifyOtp(OtpVerifyRequest request) {

        OtpVerification verification = otpVerificationRepository
                .findTopByEmailOrderByIdDesc(request.getEmail())
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        if (!verification.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        verification.setVerified(true);
        otpVerificationRepository.save(verification);

        return Map.of("message", "OTP verified successfully");
    }

    public void ensureEmailVerified(String email) {

        OtpVerification verification = otpVerificationRepository
                .findTopByEmailAndVerifiedTrueOrderByIdDesc(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Please verify your email OTP before registering"
                        )
                );

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "OTP verification expired. Please request a new OTP"
            );
        }
    }

    private String generateOtp() {
        int value = 100000 + new Random().nextInt(900000);
        return String.valueOf(value);
    }
}
