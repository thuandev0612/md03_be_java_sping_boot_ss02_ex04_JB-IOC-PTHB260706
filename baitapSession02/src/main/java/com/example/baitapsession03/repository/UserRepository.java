package com.example.baitapsession03.repository;

import com.example.baitapsession03.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserRepository {
    private final List<User> users = new ArrayList<>(List.of(
            new User(1L, "nguyenvana", "nguyenvana@example.com", "ADMIN"),
            new User(2L, "tranthib", "tranthib@example.com", "USER"),
            new User(3L, "levanc", "levanc@example.com", "USER")
    ));

    public List<User> findAll() {
        return users;
    }

    public User findById(int id) {
        for (User user : users) {
            if (user.getId().equals((long) id)) {
                return user;
            }
        }
        return null;
    }
}
