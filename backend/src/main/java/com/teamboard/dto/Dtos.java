package com.teamboard.dto;

import com.teamboard.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() { }

    public record ProjectDto(Long id, String name, String key) {
        public static ProjectDto of(Project p) { return new ProjectDto(p.getId(), p.getName(), p.getProjectKey()); }
    }
    public record ProjectRequest(@NotBlank String name, @NotBlank String key) { }

    public record SprintDto(Long id, String name, String goal, LocalDate startDate, LocalDate endDate, Long projectId) {
        public static SprintDto of(Sprint s) {
            return new SprintDto(s.getId(), s.getName(), s.getGoal(), s.getStartDate(), s.getEndDate(), s.getProject().getId());
        }
    }
    public record SprintRequest(@NotBlank String name, String goal, LocalDate startDate, LocalDate endDate) { }

    public record TaskDto(Long id, String title, String description, Task.Type type, Task.Status status,
                          Task.Priority priority, int position, String assignee, Long projectId,
                          Long sprintId, Long epicId, String epicTitle) {
        public static TaskDto of(Task t) {
            return new TaskDto(t.getId(), t.getTitle(), t.getDescription(), t.getType(), t.getStatus(),
                    t.getPriority(), t.getPosition(), t.getAssignee(), t.getProject().getId(),
                    t.getSprint() == null ? null : t.getSprint().getId(),
                    t.getEpic() == null ? null : t.getEpic().getId(),
                    t.getEpic() == null ? null : t.getEpic().getTitle());
        }
    }
    public record TaskRequest(@NotBlank String title, String description, Task.Type type, Task.Priority priority,
                              String assignee, Long sprintId, Long epicId) { }
    public record MoveRequest(@NotNull Task.Status status, int position) { }

    /** Pushed to /topic/projects/{id}/tasks. type = CREATED | UPDATED | MOVED | DELETED */
    public record TaskEvent(String type, List<TaskDto> tasks, Long deletedId) { }

    public record CommentDto(Long id, Long taskId, String author, String text, Instant createdAt) {
        public static CommentDto of(Comment c, Long taskId) {
            return new CommentDto(c.getId(), taskId, c.getAuthor(), c.getText(), c.getCreatedAt());
        }
    }
    public record CommentRequest(@NotBlank String author, @NotBlank String text) { }

    public record AttachmentDto(Long id, Long taskId, String fileName, String contentType, long size) {
        public static AttachmentDto of(Attachment a, Long taskId) {
            return new AttachmentDto(a.getId(), taskId, a.getFileName(), a.getContentType(), a.getSize());
        }
    }
    public record UrlDto(String url) { }
}
