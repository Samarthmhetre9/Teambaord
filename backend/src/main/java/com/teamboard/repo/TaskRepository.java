package com.teamboard.repo;

import com.teamboard.domain.Task;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** One query with joins instead of 1 + N lazy loads for sprint and epic. */
    @EntityGraph(attributePaths = {"sprint", "epic"})
    List<Task> findByProjectIdOrderByPositionAsc(Long projectId);

    @EntityGraph(attributePaths = {"sprint", "epic"})
    List<Task> findByProjectIdAndStatusOrderByPositionAsc(Long projectId, Task.Status status);

    @Query("select coalesce(max(t.position), -1) from Task t where t.project.id = :pid and t.status = :s")
    int maxPosition(@Param("pid") Long projectId, @Param("s") Task.Status status);

    /** Detach children when an epic is deleted. */
    @Modifying
    @Query("update Task t set t.epic = null where t.epic.id = :epicId")
    void clearEpic(@Param("epicId") Long epicId);
}
