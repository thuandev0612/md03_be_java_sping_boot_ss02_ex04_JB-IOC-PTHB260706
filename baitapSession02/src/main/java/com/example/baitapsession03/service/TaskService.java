package com.example.baitapsession03.service;

import com.example.baitapsession03.model.Task;
import com.example.baitapsession03.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    @Autowired
    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }

    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }

    public Task createTask(Task newTask) {
        if (newTask.getTitle() == null || newTask.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tiêu đề công việc không được để trống.");
        }
        if (newTask.getAssignedTo() == null) {
            throw new IllegalArgumentException("assignedTo phải là ID của người được giao việc.");
        }

        // Chi luu khi ID assignedTo tham chieu den mot User dang ton tai.
        if (userService.findUserById(newTask.getAssignedTo()) == null) {
            throw new IllegalArgumentException("Người được giao việc không tồn tại.");
        }
        if (newTask.getId() != null && newTask.getId() <= 0) {
            throw new IllegalArgumentException("ID công việc phải lớn hơn 0.");
        }

        return taskRepository.save(newTask);
    }
}
