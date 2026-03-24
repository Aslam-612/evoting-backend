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

    @Column
    private Integer age;

    @Column
    private String gender;

    @Column(name = "group_name")
    private String groupName;

    @Column
    private String constituency;

    @Column
    private String position;

    @Column
    private String phone;

    @Column
    private String photo;

    @Column
    private String description;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "vote_count")
    private Long voteCount = 0L;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;
}