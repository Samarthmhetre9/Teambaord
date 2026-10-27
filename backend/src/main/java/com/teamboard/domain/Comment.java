package com.teamboard.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name = "comments", indexes = @Index(columnList = "task_id"))
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Task task;
    @Column(nullable = false) private String author;
    @Column(nullable = false, length = 2000) private String text;
    private Instant createdAt = Instant.now();
}
