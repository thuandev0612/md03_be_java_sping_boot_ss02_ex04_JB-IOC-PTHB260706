package com.example.baitapsession03.controller;

import com.example.baitapsession03.model.User;
import com.example.baitapsession03.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    // Spring tiem Service vao Controller thong qua constructor.
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> findAllUsers(
            @RequestParam(name = "search", defaultValue = "") String search) {
        List<User> users = userService.findAllUsers();
        String keyword = search.trim().toLowerCase(Locale.ROOT);

        // Model User cua bai truoc dung username lam ten nguoi dung.
        if (!keyword.isEmpty()) {
            users = users.stream()
                    .filter(user -> user.getUsername().toLowerCase(Locale.ROOT).contains(keyword))
                    .toList();
        }

        return ResponseEntity.ok(users);
    }
}
