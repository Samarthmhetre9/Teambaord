package com.teamboard.config;

import com.teamboard.domain.*;
import com.teamboard.repo.*;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Creates a demo board on first start so the UI is not empty. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final ProjectRepository projects;
    private final SprintRepository sprints;
    private final TaskRepository tasks;

    public DataSeeder(ProjectRepository p, SprintRepository s, TaskRepository t) { projects = p; sprints = s; tasks = t; }

    @Override public void run(String... args) {
        if (projects.count() > 0) return;
        Project p = new Project(); p.setName("Website Revamp"); p.setProjectKey("WEB"); projects.save(p);
        Sprint s = new Sprint(); s.setName("Sprint 1"); s.setGoal("Ship the new landing page");
        s.setStartDate(LocalDate.now()); s.setEndDate(LocalDate.now().plusDays(14)); s.setProject(p); sprints.save(s);
        Task epic = make(p, null, null, "Landing page", Task.Type.EPIC, Task.Status.IN_PROGRESS, 0, "asha");
        make(p, s, epic, "Design hero section", Task.Type.STORY, Task.Status.TODO, 0, "ravi");
        make(p, s, epic, "Build responsive navbar", Task.Type.TASK, Task.Status.IN_PROGRESS, 1, "asha");
        make(p, s, epic, "Fix footer overlap on mobile", Task.Type.BUG, Task.Status.REVIEW, 0, "meera");
        make(p, s, epic, "Set up CI pipeline", Task.Type.TASK, Task.Status.DONE, 0, "ravi");
    }

    private Task make(Project p, Sprint s, Task epic, String title, Task.Type type, Task.Status st, int pos, String who) {
        Task t = new Task(); t.setProject(p); t.setSprint(s); t.setEpic(epic); t.setTitle(title);
        t.setType(type); t.setStatus(st); t.setPosition(pos); t.setAssignee(who);
        return tasks.save(t);
    }
}
