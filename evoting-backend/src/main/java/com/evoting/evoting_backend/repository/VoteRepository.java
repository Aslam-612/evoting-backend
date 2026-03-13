package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByVoterIdAndElectionId(Long voterId, Long electionId);
    List<Vote> findByElectionId(Long electionId);
    Optional<Vote> findByReceiptId(String receiptId);
}