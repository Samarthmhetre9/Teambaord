package com.teamboard.web;

import static com.teamboard.dto.Dtos.*;

import com.teamboard.domain.*;
import com.teamboard.repo.*;
import com.teamboard.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class BoardController {
    private final ProjectRepository projects;
    private final SprintRepository sprints;
    private final TaskService taskService;

    public BoardController(ProjectRepository projects, SprintRepository sprints, TaskService taskService) {
        this.projects = projects; this.sprints = sprints; this.taskService = taskService;
    }

    @GetMapping("/projects")
    public List<ProjectDto> projects() { return projects.findAll().stream().map(ProjectDto::of).toList(); }

    @PostMapping("/projects") @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto createProject(@Valid @RequestBody ProjectRequest r) {
        Project p = new Project(); p.setName(r.name()); p.setProjectKey(r.key().toUpperCase());
        return ProjectDto.of(projects.save(p));
    }

    @GetMapping("/projects/{id}/sprints")
    public List<SprintDto> sprints(@PathVariable Long id) {
        return sprints.findByProjectIdOrderByIdDesc(id).stream().map(SprintDto::of).toList();
    }

    @PostMapping("/projects/{id}/sprints") @ResponseStatus(HttpStatus.CREATED)
    public SprintDto createSprint(@PathVariable Long id, @Valid @RequestBody SprintRequest r) {
        Project p = projects.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Sprint s = new Sprint(); s.setProject(p); s.setName(r.name()); s.setGoal(r.goal());
        s.setStartDate(r.startDate()); s.setEndDate(r.endDate());
        return SprintDto.of(sprints.save(s));
    }

    @GetMapping("/projects/{id}/tasks")
    public List<TaskDto> tasks(@PathVariable Long id) { return taskService.list(id); }

    @PostMapping("/projects/{id}/tasks") @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@PathVariable Long id, @Valid @RequestBody TaskRequest r) { return taskService.create(id, r); }

    @PutMapping("/tasks/{id}")
    public TaskDto updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest r) { return taskService.update(id, r); }

    @PatchMapping("/tasks/{id}/move")
    public List<TaskDto> move(@PathVariable Long id, @Valid @RequestBody MoveRequest r) { return taskService.move(id, r); }

    @DeleteMapping("/tasks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { taskService.delete(id); }
}
