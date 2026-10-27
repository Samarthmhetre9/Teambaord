package com.teamboard.repo;

import com.teamboard.domain.Sprint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProjectIdOrderByIdDesc(Long projectId);
}
