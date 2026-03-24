package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "elections")
@Data
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false)
    private String status = "UPCOMING";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    private String description;

    @Column(name = "constituency")
    private String constituency;

    @Column(name = "parties", columnDefinition = "LONGTEXT")
    private String parties;
}