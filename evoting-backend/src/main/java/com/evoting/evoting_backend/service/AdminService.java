package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.*;
import com.evoting.evoting_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final VoterRepository voterRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);

    // Admin login
    public String login(String username, String password) {
        return adminRepository.findByUsername(username)
                .filter(a -> passwordEncoder.matches(password, a.getPasswordHash()))
                .map(a -> "SUCCESS")
                .orElse("INVALID");
    }

    // Create default admin (run once)
    public void createDefaultAdmin() {
        if (!adminRepository.existsByUsername("admin")) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setRole("SUPER_ADMIN");
            adminRepository.save(admin);
        }
    }

    // Voter management
    public Voter addVoter(Voter voter) {
        return voterRepository.save(voter);
    }

    public List<Voter> getAllVoters() {
        return voterRepository.findAll();
    }

    public void deleteVoter(Long id) {
        voterRepository.deleteById(id);
    }

    // Election management
    public Election addElection(Election election) {
        return electionRepository.save(election);
    }

    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }

    public Election updateElectionStatus(Long id, String status) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
        election.setStatus(status);
        return electionRepository.save(election);
    }

    public void deleteElection(Long id) {
        electionRepository.deleteById(id);
    }

    // Candidate management
    public Candidate addCandidate(Candidate candidate) {
        return candidateRepository.save(candidate);
    }

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    public void deleteCandidate(Long id) {
        candidateRepository.deleteById(id);
    }

    // Results
    public List<Candidate> getResults(Long electionId) {
        return candidateRepository.findByElectionId(electionId);
    }

    // Dashboard stats
    public java.util.Map<String, Long> getDashboardStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("totalVoters", voterRepository.count());
        stats.put("totalElections", electionRepository.count());
        stats.put("totalVotes", voteRepository.count());
        stats.put("totalCandidates", candidateRepository.count());
        return stats;
    }
}