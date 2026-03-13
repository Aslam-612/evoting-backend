package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "candidates")
@Data
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String photo;

    private String description;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "vote_count")
    private Long voteCount = 0L;
}