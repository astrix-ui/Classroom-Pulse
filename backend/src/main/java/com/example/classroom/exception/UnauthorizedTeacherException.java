package com.example.classroom.exception;

public class UnauthorizedTeacherException extends RuntimeException {
    public UnauthorizedTeacherException(String message) {
        super(message);
    }
}

