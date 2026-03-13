package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.TwilioConfig;
import com.evoting.evoting_backend.model.OtpLog;
import com.evoting.evoting_backend.repository.OtpLogRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final TwilioConfig twilioConfig;
    private final OtpLogRepository otpLogRepository;

    public String generateAndSendOtp(String mobile) {

        // Check if mobile exists in voters table (handled in controller)

        // Generate 6-digit OTP using SecureRandom
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
        otpLog.setOtpHash(otp); // In production, hash this
        otpLog.setExpiry(LocalDateTime.now().plusMinutes(5));
        otpLog.setAttempts(1);
        otpLog.setStatus("PENDING");
        otpLogRepository.save(otpLog);

        // Send SMS via Twilio
        try {
            Message.creator(
                    new PhoneNumber("+91" + mobile),
                    new PhoneNumber(twilioConfig.getPhoneNumber()),
                    "Your eVoting OTP is: " + otp + ". Valid for 5 minutes. Do not share with anyone."
            ).create();
            System.out.println("OTP sent via SMS to " + mobile);
        } catch (Exception e) {
            System.out.println("⚠️ Twilio error (using console OTP instead): " + e.getMessage());
            System.out.println("🔑 OTP for " + mobile + " is: " + otp);
        }

        return "SENT";
    }

    public boolean verifyOtp(String mobile, String otp) {
        Optional<OtpLog> otpLog = otpLogRepository
                .findTopByMobileOrderByCreatedAtDesc(mobile);

        if (otpLog.isEmpty()) return false;

        OtpLog log = otpLog.get();

        // Check expiry
        if (log.getExpiry().isBefore(LocalDateTime.now())) {
            log.setStatus("EXPIRED");
            otpLogRepository.save(log);
            return false;
        }

        // Check OTP match
        if (log.getOtpHash().equals(otp)) {
            log.setStatus("VERIFIED");
            otpLogRepository.save(log);
            return true;
        }

        // Increment attempts
        log.setAttempts(log.getAttempts() + 1);
        otpLogRepository.save(log);
        return false;
    }
}