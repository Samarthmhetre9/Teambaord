package com.teamboard.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/** One table for epics, stories, tasks and bugs. A story/task points at its epic via the self-reference. */
@Entity @Getter @Setter @NoArgsConstructor
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_board", columnList = "project_id,status,position"),
        @Index(name = "idx_task_sprint", columnList = "sprint_id"),
        @Index(name = "idx_task_epic", columnList = "epic_id")})
public class Task {
    public enum Status { TODO, IN_PROGRESS, REVIEW, DONE }
    public enum Type { EPIC, STORY, TASK, BUG }
    public enum Priority { LOW, MEDIUM, HIGH }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    @Column(length = 4000) private String description;
    @Enumerated(EnumType.STRING) private Type type = Type.TASK;
    @Enumerated(EnumType.STRING) private Status status = Status.TODO;
    @Enumerated(EnumType.STRING) private Priority priority = Priority.MEDIUM;
    private int position;
    private String assignee;
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Project project;
    @ManyToOne(fetch = FetchType.LAZY) private Sprint sprint;
    @ManyToOne(fetch = FetchType.LAZY) private Task epic;
}
