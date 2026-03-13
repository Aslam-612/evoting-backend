package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"voter_id", "election_id"}
        ))
@Data
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "encrypted_vote", nullable = false)
    private String encryptedVote;

    @Column(name = "receipt_id", unique = true)
    private String receiptId;

    @Column(name = "voted_at")
    private LocalDateTime votedAt = LocalDateTime.now();
}