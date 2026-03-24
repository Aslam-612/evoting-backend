package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.Candidate;
import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.repository.CandidateRepository;
import com.evoting.evoting_backend.repository.ElectionRepository;
import com.evoting.evoting_backend.service.JwtService;
import com.evoting.evoting_backend.service.VotingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voting")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VotingController {

    private final VotingService votingService;
    private final JwtService jwtService;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;

    // Get all active elections
    @GetMapping("/elections")
    public ResponseEntity<List<Election>> getActiveElections() {
        return ResponseEntity.ok(
                electionRepository.findByStatus("ACTIVE")
        );
    }

    // Get candidates for an election
    @GetMapping("/elections/{electionId}/candidates")
    public ResponseEntity<List<Candidate>> getCandidates(
            @PathVariable Long electionId) {
        return ResponseEntity.ok(
                candidateRepository.findByElectionId(electionId)
        );
    }

    // Cast vote
    @PostMapping("/cast")
    public ResponseEntity<Map<String, String>> castVote(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> request) {

        Map<String, String> response = new HashMap<>();

        try {
            // Extract JWT token
            String token = authHeader.replace("Bearer ", "");
            if (!jwtService.validateToken(token)) {
                response.put("status", "ERROR");
                response.put("message", "Invalid or expired session.");
                return ResponseEntity.status(401).body(response);
            }

            String mobile = jwtService.extractMobile(token);
            Long electionId = request.get("electionId");
            Long candidateId = request.get("candidateId");

            String result = votingService.castVote(mobile, electionId, candidateId);

            switch (result) {
                case "ALREADY_VOTED" -> {
                    response.put("status", "ERROR");
                    response.put("message", "You have already voted in this election.");
                }
                case "ELECTION_NOT_ACTIVE" -> {
                    response.put("status", "ERROR");
                    response.put("message", "This election is not currently active.");
                }
                case "INVALID_CANDIDATE" -> {
                    response.put("status", "ERROR");
                    response.put("message", "Invalid candidate for this election.");
                }
                case "WRONG_STATE" -> {
                    response.put("status", "ERROR");
                    response.put("message", "You are not eligible to vote in this election. This election is for a different state.");
                }
                case "WRONG_CITY" -> {
                    response.put("status", "ERROR");
                    response.put("message", "You are not eligible. This election is for a different city.");
                }
                case "WRONG_CONSTITUENCY" -> {
                    response.put("status", "ERROR");
                    response.put("message", "You can only vote for candidates in your own constituency.");
                }
                default -> {
                    response.put("status", "SUCCESS");
                    response.put("message", "Vote cast successfully!");
                    response.put("receiptId", result);
                }
            }
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Something went wrong: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }
}