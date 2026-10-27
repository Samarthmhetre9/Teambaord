package com.teamboard.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name = "sprints", indexes = @Index(columnList = "project_id"))
public class Sprint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Project project;
}
