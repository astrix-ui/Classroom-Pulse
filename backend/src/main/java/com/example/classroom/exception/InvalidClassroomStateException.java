package com.example.classroom.exception;

public class InvalidClassroomStateException extends RuntimeException {
    public InvalidClassroomStateException(String message) {
        super(message);
    }
}

