package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.repository.CandidateRepository;
import com.evoting.evoting_backend.repository.VoterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicController {

    private final VoterRepository voterRepository;
    private final CandidateRepository candidateRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getPublicStats() {
        long totalVoters = voterRepository.count();
        long votesCast = candidateRepository.sumAllVotes();
        return ResponseEntity.ok(Map.of(
                "totalVoters", totalVoters,
                "votesCast", votesCast
        ));
    }
}