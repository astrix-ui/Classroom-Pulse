package com.example.classroom.exception;

public class DuplicateStudentException extends RuntimeException {
    public DuplicateStudentException(String studentName) {
        super("A student with name '" + studentName + "' has already joined this classroom. Please choose a different name.");
    }
}

