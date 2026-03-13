package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.*;
import com.evoting.evoting_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VotingService {

    private final VoteRepository voteRepository;
    private final VoterRepository voterRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionRepository electionRepository;
    private final EncryptionService encryptionService;

    @Transactional
    public String castVote(String mobile, Long electionId, Long candidateId)
            throws Exception {

        // Get voter
        Voter voter = voterRepository.findByMobile(mobile)
                .orElseThrow(() -> new RuntimeException("Voter not found"));

        // Check already voted
        if (voteRepository.existsByVoterIdAndElectionId(
                voter.getId(), electionId)) {
            return "ALREADY_VOTED";
        }

        // Check election exists and is active
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new RuntimeException("Election not found"));

        if (!election.getStatus().equals("ACTIVE")) {
            return "ELECTION_NOT_ACTIVE";
        }

        // Check candidate belongs to election
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        if (!candidate.getElectionId().equals(electionId)) {
            return "INVALID_CANDIDATE";
        }

        // Encrypt vote data
        String voteData = "voter:" + voter.getId() +
                "|election:" + electionId +
                "|candidate:" + candidateId +
                "|time:" + LocalDateTime.now();
        String encryptedVote = encryptionService.encrypt(voteData);

        // Generate receipt ID
        String receiptId = UUID.randomUUID().toString().toUpperCase()
                .replace("-", "").substring(0, 12);

        // Save vote (atomic transaction)
        Vote vote = new Vote();
        vote.setVoterId(voter.getId());
        vote.setElectionId(electionId);
        vote.setCandidateId(candidateId);
        vote.setEncryptedVote(encryptedVote);
        vote.setReceiptId(receiptId);
        voteRepository.save(vote);

        // Update voter has_voted flag
        voter.setHasVoted(true);
        voterRepository.save(voter);

        // Update candidate vote count
        candidate.setVoteCount(candidate.getVoteCount() + 1);
        candidateRepository.save(candidate);

        return receiptId;
    }
}