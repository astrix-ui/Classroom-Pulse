package com.example.classroom.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StudentStatus {
    private final String studentId;
    private final String studentName;
    private volatile CompletionStatus status;
    private volatile boolean connected;
    private final Instant joinedAt;
    private volatile Instant completedAt;
    private final Map<String, Instant> answeredQuestions = new ConcurrentHashMap<>();
    private final Map<String, String> questionAnswers = new ConcurrentHashMap<>();
    private volatile String groupId;
    private volatile String groupName;

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
        return markCompleted(null, null, completionTime);
    }

    public synchronized boolean markCompleted(String questionId, Instant completionTime) {
        return markCompleted(questionId, null, completionTime);
    }

    public synchronized boolean markCompleted(String questionId, String answerText, Instant completionTime) {
        if (this.status == CompletionStatus.COMPLETED && questionId != null && answeredQuestions.containsKey(questionId)) {
            return false; // already completed (idempotency check)
        }
        this.status = CompletionStatus.COMPLETED;
        this.completedAt = completionTime;
        if (questionId != null) {
            this.answeredQuestions.put(questionId, completionTime);
            if (answerText != null && !answerText.trim().isEmpty()) {
                this.questionAnswers.put(questionId, answerText.trim());
            }
        }
        return true;
    }

    public Map<String, Instant> getAnsweredQuestions() {
        return Collections.unmodifiableMap(answeredQuestions);
    }

    public Map<String, String> getQuestionAnswers() {
        return Collections.unmodifiableMap(questionAnswers);
    }

    public synchronized void removeAnsweredQuestion(String questionId) {
        if (questionId != null) {
            this.answeredQuestions.remove(questionId);
            this.questionAnswers.remove(questionId);
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public synchronized void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public synchronized void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}

