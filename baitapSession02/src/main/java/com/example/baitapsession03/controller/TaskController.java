package com.example.baitapsession03.controller;

import com.example.baitapsession03.model.Task;
import com.example.baitapsession03.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<Task>> findAllTasks(
            @RequestParam(name = "search", defaultValue = "") String search) {
        List<Task> tasks = taskService.findAllTasks();
        String keyword = search.trim().toLowerCase(Locale.ROOT);

        // Loc tren ket qua cua Service, khong xoa du lieu trong Repository.
        if (!keyword.isEmpty()) {
            tasks = tasks.stream()
                    .filter(task -> task.getTitle().toLowerCase(Locale.ROOT).contains(keyword))
                    .toList();
        }

        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task newTask) {
        try {
            Task savedTask = taskService.createTask(newTask);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }
}
