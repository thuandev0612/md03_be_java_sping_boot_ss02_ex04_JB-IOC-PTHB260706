package com.example.baitapsession03.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private Long id;
    private String title;
    private String description;
    private String priority;
    // ID cua User duoc giao viec, vi du JSON: "assignedTo": 1.
    private Integer assignedTo;
}
