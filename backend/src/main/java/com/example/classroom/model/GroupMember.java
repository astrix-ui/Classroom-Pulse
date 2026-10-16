package com.example.classroom.model;

import java.time.Instant;

public class GroupMember {
    private final String studentId;
    private final String studentName;
    private final Instant joinedAt;

    public GroupMember(String studentId, String studentName, Instant joinedAt) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.joinedAt = joinedAt;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}

