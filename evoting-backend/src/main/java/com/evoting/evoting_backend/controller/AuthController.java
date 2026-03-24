package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.Voter;
import com.evoting.evoting_backend.repository.VoterRepository;
import com.evoting.evoting_backend.service.JwtService;
import com.evoting.evoting_backend.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8081")public class AuthController {

    private final OtpService otpService;
    private final JwtService jwtService;
    private final VoterRepository voterRepository;

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(
            @RequestBody Map<String, String> request) {

        String aadhar = request.get("aadhar");
        Map<String, String> response = new HashMap<>();

        Optional<Voter> voter = voterRepository.findByAadharNumber(aadhar);
        if (voter.isEmpty()) {
            response.put("status", "ERROR");
            response.put("message", "Aadhar number not registered. Contact admin.");
            return ResponseEntity.badRequest().body(response);
        }

        String mobile = voter.get().getMobile();
        String result = otpService.generateAndSendOtp(mobile);

        if (result.equals("MAX_ATTEMPTS")) {
            response.put("status", "ERROR");
            response.put("message", "Maximum OTP attempts reached. Try after 5 minutes.");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("status", "SUCCESS");
        response.put("message", "OTP sent to your registered mobile number.");
        response.put("mobile", mobile.substring(0, 2) + "XXXXXXXX");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @RequestBody Map<String, String> request) {

        String aadhar = request.get("aadhar");
        String otp = request.get("otp");
        Map<String, String> response = new HashMap<>();

        Optional<Voter> voterOpt = voterRepository.findByAadharNumber(aadhar);
        if (voterOpt.isEmpty()) {
            response.put("status", "ERROR");
            response.put("message", "Aadhar number not found.");
            return ResponseEntity.badRequest().body(response);
        }

        Voter voter = voterOpt.get();
        String mobile = voter.getMobile();
        boolean isValid = otpService.verifyOtp(mobile, otp);

        if (!isValid) {
            response.put("status", "ERROR");
            response.put("message", "Invalid or expired OTP.");
            return ResponseEntity.badRequest().body(response);
        }
        // Check voter age - must be 18+
        if (voter.getDateOfBirth() != null) {
            try {
                java.time.LocalDate dob = java.time.LocalDate.parse(voter.getDateOfBirth());
                int age = java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
                if (age < 18) {
                    response.put("status", "ERROR");
                    response.put("message", "You must be 18 or older to vote.");
                    return ResponseEntity.badRequest().body(response);
                }
            } catch (Exception e) {
                System.out.println("DOB parse error: " + e.getMessage());
            }
        }
        String token = jwtService.generateToken(mobile, "VOTER");
        response.put("status", "SUCCESS");
        response.put("token", token);
        response.put("message", "Login successful!");
        response.put("voterName", voter.getName());
        response.put("hasVoted", voter.getHasVoted().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{aadhar}")
    public ResponseEntity<Map<String, Object>> checkAadhar(
            @PathVariable String aadhar) {
        Map<String, Object> response = new HashMap<>();
        boolean exists = voterRepository.existsByAadharNumber(aadhar);
        response.put("registered", exists);
        return ResponseEntity.ok(response);
    }
}