package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "voters")
@Data
public class Voter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String mobile;

    @Column(name = "voter_id_number")
    private String voterIdNumber;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "has_voted")
    private Boolean hasVoted = false;

    @Column(name = "election_id")
    private Long electionId;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Column(name = "is_active")
    private Boolean isActive = true;
}