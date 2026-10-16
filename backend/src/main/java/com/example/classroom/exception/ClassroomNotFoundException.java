package com.example.classroom.exception;

public class ClassroomNotFoundException extends RuntimeException {
    public ClassroomNotFoundException(String roomCode) {
        super("Classroom not found with room code: " + roomCode);
    }
}

