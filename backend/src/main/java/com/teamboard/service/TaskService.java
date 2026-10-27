package com.teamboard.service;

import static com.teamboard.dto.Dtos.*;

import com.teamboard.domain.*;
import com.teamboard.repo.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final SprintRepository sprints;
    private final CommentRepository comments;
    private final AttachmentRepository attachments;
    private final SimpMessagingTemplate ws;

    public TaskService(TaskRepository tasks, ProjectRepository projects, SprintRepository sprints,
                       CommentRepository comments, AttachmentRepository attachments, SimpMessagingTemplate ws) {
        this.tasks = tasks; this.projects = projects; this.sprints = sprints;
        this.comments = comments; this.attachments = attachments; this.ws = ws;
    }

    @Transactional(readOnly = true)
    public List<TaskDto> list(Long projectId) {
        return tasks.findByProjectIdOrderByPositionAsc(projectId).stream().map(TaskDto::of).toList();
    }

    public TaskDto create(Long projectId, TaskRequest r) {
        Project p = projects.findById(projectId).orElseThrow(() -> notFound("Project"));
        Task t = new Task();
        t.setProject(p);
        apply(t, r);
        t.setPosition(tasks.maxPosition(projectId, Task.Status.TODO) + 1);
        tasks.save(t);
        return publish("CREATED", projectId, List.of(t));
    }

    public TaskDto update(Long id, TaskRequest r) {
        Task t = get(id);
        apply(t, r);
        return publish("UPDATED", t.getProject().getId(), List.of(t));
    }

    /** Drag-and-drop: put task at `position` inside column `status`, then renumber both affected columns. */
    public List<TaskDto> move(Long id, MoveRequest r) {
        Task t = get(id);
        Long pid = t.getProject().getId();
        Task.Status old = t.getStatus();

        List<Task> target = new ArrayList<>(tasks.findByProjectIdAndStatusOrderByPositionAsc(pid, r.status()));
        target.removeIf(x -> x.getId().equals(id));
        int idx = Math.max(0, Math.min(r.position(), target.size()));
        t.setStatus(r.status());
        target.add(idx, t);
        renumber(target);

        List<Task> changed = new ArrayList<>(target);
        if (old != r.status()) {
            List<Task> source = new ArrayList<>(tasks.findByProjectIdAndStatusOrderByPositionAsc(pid, old));
            source.removeIf(x -> x.getId().equals(id));
            renumber(source);
            changed.addAll(source);
        }
        List<TaskDto> dtos = changed.stream().map(TaskDto::of).toList();
        ws.convertAndSend("/topic/projects/" + pid + "/tasks", new TaskEvent("MOVED", dtos, null));
        return dtos;
    }

    public void delete(Long id) {
        Task t = get(id);
        Long pid = t.getProject().getId();
        comments.deleteByTaskId(id);
        attachments.deleteByTaskId(id);
        tasks.clearEpic(id);
        tasks.delete(t);
        ws.convertAndSend("/topic/projects/" + pid + "/tasks", new TaskEvent("DELETED", List.of(), id));
    }

    // ---- helpers ----
    private void apply(Task t, TaskRequest r) {
        t.setTitle(r.title());
        t.setDescription(r.description());
        if (r.type() != null) t.setType(r.type());
        if (r.priority() != null) t.setPriority(r.priority());
        t.setAssignee(r.assignee());
        t.setSprint(r.sprintId() == null ? null : sprints.findById(r.sprintId()).orElseThrow(() -> notFound("Sprint")));
        t.setEpic(r.epicId() == null || r.epicId().equals(t.getId()) ? null : get(r.epicId()));
    }

    private void renumber(List<Task> list) {
        for (int i = 0; i < list.size(); i++) list.get(i).setPosition(i);
    }

    private TaskDto publish(String type, Long pid, List<Task> changed) {
        tasks.flush();
        List<TaskDto> dtos = changed.stream().map(TaskDto::of).toList();
        ws.convertAndSend("/topic/projects/" + pid + "/tasks", new TaskEvent(type, dtos, null));
        return dtos.get(0);
    }

    private Task get(Long id) { return tasks.findById(id).orElseThrow(() -> notFound("Task")); }
    private ResponseStatusException notFound(String what) { return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found"); }
}
