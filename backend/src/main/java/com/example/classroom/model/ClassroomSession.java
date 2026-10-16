package com.example.classroom.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassroomSession {
    private final String roomCode;
    private final String teacherToken;
    private final Instant createdAt;
    private volatile boolean active;
    private volatile Question currentQuestion;
    private final AtomicInteger sequenceCounter = new AtomicInteger(0);

    // Keyed by studentId
    private final Map<String, StudentStatus> students = new ConcurrentHashMap<>();

    public ClassroomSession(String roomCode, String teacherToken, Instant createdAt) {
        this.roomCode = roomCode;
        this.teacherToken = teacherToken;
        this.createdAt = createdAt;
        this.active = true;
        this.currentQuestion = null;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getTeacherToken() {
        return teacherToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(Question currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public int nextSequenceNumber() {
        return sequenceCounter.incrementAndGet();
    }

    public Map<String, StudentStatus> getStudents() {
        return Collections.unmodifiableMap(students);
    }

    public StudentStatus getStudent(String studentId) {
        return students.get(studentId);
    }

    public boolean hasStudentWithName(String name) {
        return students.values().stream()
                .anyMatch(s -> s.getStudentName().equalsIgnoreCase(name.trim()));
    }

    public void addStudent(StudentStatus student) {
        students.put(student.getStudentId(), student);
    }

    public StudentStatus removeStudent(String studentId) {
        return students.remove(studentId);
    }

    public void resetAllStudentsToPending() {
        students.values().forEach(StudentStatus::resetToPending);
    }
}

