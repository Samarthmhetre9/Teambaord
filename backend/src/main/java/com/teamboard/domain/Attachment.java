package com.teamboard.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name = "attachments", indexes = @Index(columnList = "task_id"))
public class Attachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Task task;
    @Column(nullable = false) private String fileName;
    @Column(nullable = false) private String storageKey;
    private String contentType;
    private long size;
}
