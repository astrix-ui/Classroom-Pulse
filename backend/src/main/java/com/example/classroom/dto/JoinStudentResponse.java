package com.example.classroom.dto;

import com.example.classroom.model.CompletionStatus;

public class JoinStudentResponse {
    private String studentId;
    private String studentName;
    private String roomCode;
    private CompletionStatus status;

    public JoinStudentResponse() {}

    public JoinStudentResponse(String studentId, String studentName, String roomCode, CompletionStatus status) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.roomCode = roomCode;
        this.status = status;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public CompletionStatus getStatus() {
        return status;
    }

    public void setStatus(CompletionStatus status) {
        this.status = status;
    }
}

