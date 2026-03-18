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

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpLogRepository otpLogRepository;

    private static final String FAST2SMS_API_KEY = "VCEbJjSKqyLD1P9Bd5UWe7MohtazsTYwk2p3xiIrGAQHfFl6Z4SIEgqX4CcjNlLFG3JoeQBADanO1yMv";

    public String generateAndSendOtp(String mobile) {

        // Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(999999));

        // Check attempt limit
        Optional<OtpLog> existing = otpLogRepository
                .findTopByMobileOrderByCreatedAtDesc(mobile);

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
        otpLog.setOtpHash(otp);
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

        if (log.getExpiry().isBefore(LocalDateTime.now())) {
            log.setStatus("EXPIRED");
            otpLogRepository.save(log);
            return false;
        }

        if (log.getOtpHash().equals(otp)) {
            log.setStatus("VERIFIED");
            otpLogRepository.save(log);
            return true;
        }

        log.setAttempts(log.getAttempts() + 1);
        otpLogRepository.save(log);
        return false;
    }
}