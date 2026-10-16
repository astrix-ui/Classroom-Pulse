package com.example.classroom.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ClassroomStateResponse {
    private String roomCode;
    private boolean active;
    private QuestionDto currentQuestion;
    private List<StudentDto> students = new ArrayList<>();
    private int totalStudents;
    private int completedCount;
    private int pendingCount;
    private double completionPercentage;
    private boolean allowAnswers = false;
    private List<QuestionDto> questions = new ArrayList<>();
    private Instant createdAt;

    public ClassroomStateResponse() {}

    public ClassroomStateResponse(String roomCode, boolean active, QuestionDto currentQuestion,
                                  List<StudentDto> students, int totalStudents, int completedCount,
                                  int pendingCount, double completionPercentage, Instant createdAt) {
        this.roomCode = roomCode;
        this.active = active;
        this.currentQuestion = currentQuestion;
        this.students = students != null ? students : new ArrayList<>();
        this.totalStudents = totalStudents;
        this.completedCount = completedCount;
        this.pendingCount = pendingCount;
        this.completionPercentage = completionPercentage;
        this.createdAt = createdAt;
        this.allowAnswers = false;
        this.questions = new ArrayList<>();
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public QuestionDto getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(QuestionDto currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public List<StudentDto> getStudents() {
        return students;
    }

    public void setStudents(List<StudentDto> students) {
        this.students = students;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAllowAnswers() {
        return allowAnswers;
    }

    public void setAllowAnswers(boolean allowAnswers) {
        this.allowAnswers = allowAnswers;
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions != null ? questions : new ArrayList<>();
    }
}

