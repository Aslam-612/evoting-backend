package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.OtpLog;
import com.evoting.evoting_backend.repository.OtpLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpLogRepository otpLogRepository;

    @Value("${fast2sms.api.key}")
    private String FAST2SMS_API_KEY;

    public String generateAndSendOtp(String mobile) {

        // Test bypass — OTP is always 123456 for this number
        if (mobile.equals("9000000000") || mobile.equals("8000000000")) {
            OtpLog otpLog = new OtpLog();
            otpLog.setMobile(mobile);
            String hashedOtp = BCrypt.hashpw("123456", BCrypt.gensalt());
            otpLog.setOtpHash(hashedOtp);
            otpLog.setExpiry(LocalDateTime.now().plusMinutes(30));
            otpLog.setAttempts(1);
            otpLog.setStatus("PENDING");
            otpLogRepository.save(otpLog);
            System.out.println("🧪 TEST MODE: OTP for " + mobile + " is: 123456");
            return "SENT";
        }

        // Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(999999));

        // Check attempt limit
        Optional<OtpLog> existing = otpLogRepository
                .findTopByMobileOrderByCreatedAtDesc(mobile);
        if (existing.isPresent()) {
            OtpLog log = existing.get();

            if (log.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(30))) {
                return "WAIT"; // block rapid requests
            }
        }

        if (existing.isPresent()) {
            OtpLog log = existing.get();
            if (log.getAttempts() >= 3 &&
                    log.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                return "MAX_ATTEMPTS";
            }
        }

        // Save OTP log
        OtpLog otpLog = new OtpLog();
        otpLog.setMobile(mobile);
        String hashedOtp = BCrypt.hashpw(otp, BCrypt.gensalt());
        otpLog.setOtpHash(hashedOtp);
        otpLog.setExpiry(LocalDateTime.now().plusMinutes(5));
        otpLog.setAttempts(1);
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

        System.out.println("STATUS: " + log.getStatus());
        System.out.println("ATTEMPTS: " + log.getAttempts());
        System.out.println("EXPIRY: " + log.getExpiry());
        System.out.println("NOW: " + LocalDateTime.now());

        if ("VERIFIED".equals(log.getStatus())) return false;

        if (log.getExpiry().isBefore(LocalDateTime.now())) {
            log.setStatus("EXPIRED");
            otpLogRepository.save(log);
            return false;
        }
        if (log.getAttempts() >= 5) {
            log.setStatus("BLOCKED");
            otpLogRepository.save(log);
            return false;
        }
        System.out.println("Entered OTP: " + otp);
        System.out.println("Stored Hash: " + log.getOtpHash());
        System.out.println("Match: " + BCrypt.checkpw(otp, log.getOtpHash()));
        if (BCrypt.checkpw(otp, log.getOtpHash())) {
            log.setStatus("VERIFIED");
            otpLogRepository.save(log);
            return true;
        }

        log.setAttempts(log.getAttempts() + 1);
        otpLogRepository.save(log);
        return false;

    }
}