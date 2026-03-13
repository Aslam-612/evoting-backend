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
@CrossOrigin(origins = "*")
public class AuthController {

    private final OtpService otpService;
    private final JwtService jwtService;
    private final VoterRepository voterRepository;

    // Step 1: Send OTP
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(
            @RequestBody Map<String, String> request) {

        String mobile = request.get("mobile");
        Map<String, String> response = new HashMap<>();

        // Check if mobile is registered
        if (!voterRepository.existsByMobile(mobile)) {
            response.put("status", "ERROR");
            response.put("message", "Mobile number not registered. Contact admin.");
            return ResponseEntity.badRequest().body(response);
        }

        // Send OTP
        String result = otpService.generateAndSendOtp(mobile);

        if (result.equals("MAX_ATTEMPTS")) {
            response.put("status", "ERROR");
            response.put("message", "Maximum OTP attempts reached. Try after 5 minutes.");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("status", "SUCCESS");
        response.put("message", "OTP sent successfully to " + mobile);
        return ResponseEntity.ok(response);
    }

    // Step 2: Verify OTP and get JWT token
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @RequestBody Map<String, String> request) {

        String mobile = request.get("mobile");
        String otp = request.get("otp");
        Map<String, String> response = new HashMap<>();

        boolean isValid = otpService.verifyOtp(mobile, otp);

        if (!isValid) {
            response.put("status", "ERROR");
            response.put("message", "Invalid or expired OTP.");
            return ResponseEntity.badRequest().body(response);
        }

        // Get voter details
        Optional<Voter> voter = voterRepository.findByMobile(mobile);
        if (voter.isEmpty()) {
            response.put("status", "ERROR");
            response.put("message", "Voter not found.");
            return ResponseEntity.badRequest().body(response);
        }

        // Generate JWT token
        String token = jwtService.generateToken(mobile, "VOTER");

        response.put("status", "SUCCESS");
        response.put("token", token);
        response.put("message", "Login successful!");
        response.put("voterName", voter.get().getName());
        response.put("hasVoted", voter.get().getHasVoted().toString());
        return ResponseEntity.ok(response);
    }

    // Check if mobile is registered
    @GetMapping("/check/{mobile}")
    public ResponseEntity<Map<String, Object>> checkMobile(
            @PathVariable String mobile) {

        Map<String, Object> response = new HashMap<>();
        boolean exists = voterRepository.existsByMobile(mobile);
        response.put("registered", exists);
        return ResponseEntity.ok(response);
    }
}