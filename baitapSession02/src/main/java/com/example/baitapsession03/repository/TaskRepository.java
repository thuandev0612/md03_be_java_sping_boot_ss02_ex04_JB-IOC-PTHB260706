package com.example.baitapsession03.repository;

import com.example.baitapsession03.model.Task;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TaskRepository {
    private final List<Task> tasks;

    public TaskRepository() {
        tasks = new ArrayList<>(List.of(
                new Task(1L, "Create project", "Initialize the Spring Boot project.", "HIGH", 1),
                new Task(2L, "Create User model", "Define id, username, email and role.", "HIGH", 2),
                new Task(3L, "Create Task model", "Define task fields and the assigned user.", "HIGH", 3),
                new Task(4L, "Create User repository", "Prepare three users in memory.", "MEDIUM", 1),
                new Task(5L, "Create Task repository", "Prepare ten tasks in memory.", "MEDIUM", 2),
                new Task(6L, "Create User service", "Retrieve users from the repository.", "MEDIUM", 3),
                new Task(7L, "Create Task service", "Retrieve tasks from the repository.", "MEDIUM", 1),
                new Task(8L, "Create controllers", "Expose endpoints for users and tasks.", "HIGH", 2),
                new Task(9L, "Test endpoints", "Check that both endpoints return JSON data.", "HIGH", 3),
                new Task(10L, "Write documentation", "Explain the three layers and constructor injection.", "LOW", 1)
        ));
    }

    public synchronized List<Task> findAll() {
        return new ArrayList<>(tasks);
    }

    // synchronized giup hai request tao task khong cap trung ID.
    public synchronized Task save(Task newTask) {
        if (newTask.getId() == null) {
            long nextId = tasks.stream()
                    .mapToLong(Task::getId)
                    .max()
                    .orElse(0L) + 1;
            newTask.setId(nextId);
        } else {
            boolean idExists = tasks.stream()
                    .anyMatch(task -> task.getId().equals(newTask.getId()));
            if (idExists) {
                throw new IllegalArgumentException("ID công việc đã tồn tại.");
            }
        }

        tasks.add(newTask);
        return newTask;
    }
}
