package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.OtpLog;
import com.evoting.evoting_backend.repository.OtpLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpLogRepository otpLogRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${fast2sms.api.key}")
    private String FAST2SMS_API_KEY;

    public String generateAndSendOtp(String mobile) {

        // Test bypass — OTP is always 123456 for test numbers
        if (mobile.equals("9000000000") || mobile.equals("8000000000")) {
            OtpLog otpLog = new OtpLog();
            otpLog.setMobile(mobile);
            otpLog.setOtpHash(passwordEncoder.encode("123456"));
            otpLog.setExpiry(LocalDateTime.now().plusMinutes(30));
            otpLog.setAttempts(0);
            otpLog.setStatus("PENDING");
            otpLogRepository.save(otpLog);
            System.out.println("🧪 TEST MODE: OTP for " + mobile + " is: 123456");
            return "SENT";
        }

        // Check existing OTP log
        Optional<OtpLog> existing = otpLogRepository
                .findTopByMobileOrderByCreatedAtDesc(mobile);

        if (existing.isPresent()) {
            OtpLog log = existing.get();

            // Block rapid resend requests (30 second cooldown)
            if (log.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(30))) {
                return "WAIT";
            }

            // Block if max attempts reached within 5 minutes
            if (log.getAttempts() >= 3 &&
                    log.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                return "MAX_ATTEMPTS";
            }
        }

        // Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(999999));

        // Save hashed OTP log
        OtpLog otpLog = new OtpLog();
        otpLog.setMobile(mobile);
        otpLog.setOtpHash(passwordEncoder.encode(otp));
        otpLog.setExpiry(LocalDateTime.now().plusMinutes(5));
        otpLog.setAttempts(0);
        otpLog.setStatus("PENDING");
        otpLogRepository.save(otpLog);

        // Send SMS via Fast2SMS
        try {
            String url = "https://www.fast2sms.com/dev/bulkV2"
                    + "?authorization=" + FAST2SMS_API_KEY
                    + "&sender_id=FSTSMS"
                    + "&message=Your+eVoting+OTP+is+" + otp + ".+Valid+for+5+minutes.+Do+not+share."
                    + "&language=english"
                    + "&route=q"
                    + "&numbers=" + mobile;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("cache-control", "no-cache")
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Fast2SMS response: " + response.body());
            System.out.println("✅ OTP sent to " + mobile);
            System.out.println("🔑 OTP for " + mobile + " is: " + otp);
        } catch (Exception e) {
            System.out.println("⚠️ Fast2SMS error: " + e.getMessage());
            System.out.println("🔑 OTP for " + mobile + " is: " + otp);
        }

        return "SENT";
    }

    public boolean verifyOtp(String mobile, String otp) {
        Optional<OtpLog> otpLog = otpLogRepository
                .findTopByMobileOrderByCreatedAtDesc(mobile);

        if (otpLog.isEmpty()) return false;

        OtpLog log = otpLog.get();

        // Already used
        if ("VERIFIED".equals(log.getStatus())) return false;

        // Expired
        if (log.getExpiry().isBefore(LocalDateTime.now())) {
            log.setStatus("EXPIRED");
            otpLogRepository.save(log);
            return false;
        }

        // Already blocked
        if ("BLOCKED".equals(log.getStatus())) return false;

        // Correct OTP
        if (passwordEncoder.matches(otp, log.getOtpHash())) {
            log.setStatus("VERIFIED");
            otpLogRepository.save(log);
            return true;
        }

        // Wrong OTP — increment attempts
        log.setAttempts(log.getAttempts() + 1);

        // Block after 3 wrong attempts
        if (log.getAttempts() >= 3) {
            log.setStatus("BLOCKED");
        }

        otpLogRepository.save(log);
        return false;
    }
}