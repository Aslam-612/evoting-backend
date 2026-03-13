package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.*;
import com.evoting.evoting_backend.service.AdminService;
import com.evoting.evoting_backend.service.JwtService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final JwtService jwtService;

    // Create default admin on startup
    @PostConstruct
    public void init() {
        adminService.createDefaultAdmin();
    }

    // Admin login
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> request) {

        Map<String, String> response = new HashMap<>();
        String result = adminService.login(
                request.get("username"),
                request.get("password")
        );

        if (result.equals("SUCCESS")) {
            String token = jwtService.generateToken(
                    request.get("username"), "ADMIN"
            );
            response.put("status", "SUCCESS");
            response.put("token", token);
            response.put("message", "Admin login successful!");
        } else {
            response.put("status", "ERROR");
            response.put("message", "Invalid username or password.");
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // Dashboard stats
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Long>> getDashboard(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ── Voter Management ──────────────────────────────

    @GetMapping("/voters")
    public ResponseEntity<List<Voter>> getVoters(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getAllVoters());
    }

    @PostMapping("/voters")
    public ResponseEntity<Voter> addVoter(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Voter voter) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.addVoter(voter));
    }

    @DeleteMapping("/voters/{id}")
    public ResponseEntity<Map<String, String>> deleteVoter(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        adminService.deleteVoter(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Voter deleted.");
        return ResponseEntity.ok(response);
    }

    // ── Election Management ───────────────────────────

    @GetMapping("/elections")
    public ResponseEntity<List<Election>> getElections(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getAllElections());
    }

    @PostMapping("/elections")
    public ResponseEntity<Election> addElection(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Election election) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.addElection(election));
    }

    @PutMapping("/elections/{id}/status")
    public ResponseEntity<Election> updateStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                adminService.updateElectionStatus(id, body.get("status"))
        );
    }

    @DeleteMapping("/elections/{id}")
    public ResponseEntity<Map<String, String>> deleteElection(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        adminService.deleteElection(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Election deleted.");
        return ResponseEntity.ok(response);
    }

    // ── Candidate Management ──────────────────────────

    @GetMapping("/candidates")
    public ResponseEntity<List<Candidate>> getCandidates(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getAllCandidates());
    }

    @PostMapping("/candidates")
    public ResponseEntity<Candidate> addCandidate(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Candidate candidate) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.addCandidate(candidate));
    }

    @DeleteMapping("/candidates/{id}")
    public ResponseEntity<Map<String, String>> deleteCandidate(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        adminService.deleteCandidate(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Candidate deleted.");
        return ResponseEntity.ok(response);
    }

    // ── Results ───────────────────────────────────────

    @GetMapping("/results/{electionId}")
    public ResponseEntity<List<Candidate>> getResults(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long electionId) {
        if (!isValidAdmin(authHeader))
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getResults(electionId));
    }

    // ── Helper ────────────────────────────────────────

    private boolean isValidAdmin(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            return jwtService.validateToken(token) &&
                    jwtService.extractRole(token).equals("ADMIN");
        } catch (Exception e) {
            return false;
        }
    }
}