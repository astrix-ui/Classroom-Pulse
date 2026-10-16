package com.example.classroom.model;

import java.time.Instant;

public class StudentStatus {
    private final String studentId;
    private final String studentName;
    private volatile CompletionStatus status;
    private volatile boolean connected;
    private final Instant joinedAt;
    private volatile Instant completedAt;

    public StudentStatus(String studentId, String studentName, Instant joinedAt) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.status = CompletionStatus.PENDING;
        this.connected = true;
        this.joinedAt = joinedAt;
        this.completedAt = null;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public CompletionStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(CompletionStatus status) {
        this.status = status;
    }

    public boolean isConnected() {
        return connected;
    }

    public synchronized void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public synchronized void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public synchronized void resetToPending() {
        this.status = CompletionStatus.PENDING;
        this.completedAt = null;
    }

    public synchronized boolean markCompleted(Instant completionTime) {
        if (this.status == CompletionStatus.COMPLETED) {
            return false; // already completed (idempotency check)
        }
        this.status = CompletionStatus.COMPLETED;
        this.completedAt = completionTime;
        return true;
    }
}

