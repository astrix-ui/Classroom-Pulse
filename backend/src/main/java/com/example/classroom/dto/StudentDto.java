package com.example.classroom.dto;

import com.example.classroom.model.CompletionStatus;
import com.example.classroom.model.StudentStatus;

import java.time.Instant;

public class StudentDto {
    private String studentId;
    private String studentName;
    private CompletionStatus status;
    private boolean connected;
    private Instant joinedAt;
    private Instant completedAt;

    public StudentDto() {}

    public StudentDto(String studentId, String studentName, CompletionStatus status,
                      boolean connected, Instant joinedAt, Instant completedAt) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.status = status;
        this.connected = connected;
        this.joinedAt = joinedAt;
        this.completedAt = completedAt;
    }

    public static StudentDto fromDomain(StudentStatus student) {
        if (student == null) return null;
        return new StudentDto(
                student.getStudentId(),
                student.getStudentName(),
                student.getStatus(),
                student.isConnected(),
                student.getJoinedAt(),
                student.getCompletedAt()
        );
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

    public CompletionStatus getStatus() {
        return status;
    }

    public void setStatus(CompletionStatus status) {
        this.status = status;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}

