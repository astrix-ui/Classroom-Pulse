package com.example.classroom.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(String studentId) {
        super("Student not found with ID: " + studentId);
    }
}

