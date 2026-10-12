package com.example.baitapsession03.service;

import com.example.baitapsession03.model.User;
import com.example.baitapsession03.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Spring tiem Repository vao Service thong qua constructor.
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public User findUserById(int id) {
        return userRepository.findById(id);
    }
}
