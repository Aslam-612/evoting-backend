package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.Voter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VoterRepository extends JpaRepository<Voter, Long> {
    Optional<Voter> findByMobile(String mobile);
    boolean existsByMobile(String mobile);
}